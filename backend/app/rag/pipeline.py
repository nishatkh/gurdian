from __future__ import annotations

import hashlib
import json
import math
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Optional

from rank_bm25 import BM25Okapi
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.models.entities import KnowledgeChunk, KnowledgeDocument

EMBED_DIM = 128

# Bilingual synonym expansion for Dhaka/Bangladesh security queries
QUERY_EXPANSIONS = {
    "জিডি": "gd general diary police",
    "চুরি": "theft stolen snatching churi",
    "হারিয়ে": "lost missing harai",
    "হারানো": "lost missing",
    "সিম": "sim block operator grameenphone robi banglalink teletalk",
    "বিকাশ": "bkash mfs freeze 16247",
    "নগদ": "nagad mfs freeze 16167",
    "রকেট": "rocket dbbl mfs freeze 16216",
    "আইএমইআই": "imei neir btrc",
    "৯৯৯": "999 emergency police",
    "churi": "theft stolen",
    "harai": "lost missing",
}


def detect_language(text: str) -> str:
    bangla_chars = sum(1 for ch in text if "\u0980" <= ch <= "\u09FF")
    latin_chars = sum(1 for ch in text if ("a" <= ch.lower() <= "z"))
    if bangla_chars > 3 and latin_chars > 5:
        return "bilingual"
    if bangla_chars > 3:
        return "bn"
    return "en"


STOPWORDS = {
    "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "for", "from",
    "how", "i", "in", "is", "it", "my", "of", "on", "or", "should", "the", "this",
    "to", "was", "what", "when", "where", "which", "who", "why", "with", "you", "your",
}


def tokenize(text: str) -> list[str]:
    lowered = text.lower()
    for bn_kw, exp in QUERY_EXPANSIONS.items():
        if bn_kw in lowered:
            lowered = f"{lowered} {exp}"
    raw = re.findall(r"[\u0980-\u09FFa-z0-9]+", lowered)
    return [t for t in raw if t not in STOPWORDS and len(t) > 1]


def compute_hashed_embedding(text: str, dim: int = EMBED_DIM) -> list[float]:
    """Deterministic normalized character+word n-gram embedding compatible with pgvector & SQLite."""
    vec = [0.0] * dim
    tokens = tokenize(text)
    if not tokens:
        return vec
    for tok in tokens:
        h = int(hashlib.blake2b(tok.encode("utf-8"), digest_size=8).hexdigest(), 16)
        vec[h % dim] += 1.0
        if len(tok) >= 4:
            for i in range(len(tok) - 2):
                tri = tok[i : i + 3]
                ht = int(hashlib.blake2b(tri.encode("utf-8"), digest_size=8).hexdigest(), 16)
                vec[ht % dim] += 0.35
    norm = math.sqrt(sum(v * v for v in vec)) or 1.0
    return [round(v / norm, 6) for v in vec]


def cosine_similarity(a: list[float], b: list[float]) -> float:
    if not a or not b or len(a) != len(b):
        return 0.0
    return sum(x * y for x, y in zip(a, b))


def semantic_chunk_text(text: str, max_chars: int = 550) -> list[str]:
    paragraphs = [p.strip() for p in re.split(r"\n+", text) if p.strip()]
    chunks: list[str] = []
    for para in paragraphs:
        if len(para) <= max_chars:
            chunks.append(para)
        else:
            sentences = re.split(r"(?<=[.!?।])\s+", para)
            curr = ""
            for s in sentences:
                if len(curr) + len(s) + 1 <= max_chars:
                    curr = f"{curr} {s}".strip()
                else:
                    if curr:
                        chunks.append(curr)
                    curr = s
            if curr:
                chunks.append(curr)
    return chunks


@dataclass
class RetrievedChunk:
    doc_id: str
    title: str
    publisher: str
    source_url: str
    last_verified: str
    content: str
    bm25_score: float
    dense_score: float
    hybrid_score: float


class HybridRAGPipeline:
    """Hybrid BM25 + Dense Vector Reciprocal Rank Fusion retriever over verified KnowledgeDocuments."""

    def ingest_default_if_empty(self, db: Session) -> int:
        if db.query(KnowledgeDocument).count() > 0:
            return 0
        kb_file = get_settings().kb_file
        if not kb_file.exists():
            return 0
        docs = json.loads(kb_file.read_text(encoding="utf-8"))
        count = 0
        for item in docs:
            doc = KnowledgeDocument(
                id=item["doc_id"],
                title=item["title"],
                publisher=item["publisher"],
                source_url=item["source_url"],
                date_retrieved=item["date_retrieved"],
                last_verified=item["last_verified"],
                language=item.get("language", "bilingual"),
                jurisdiction=item.get("jurisdiction", "BD"),
                version=item.get("version", "1.0"),
                trust_level=item.get("trust_level", "official"),
                is_active=True,
                content=item["content"],
            )
            db.add(doc)
            db.flush()
            self.reindex_document(db, doc)
            count += 1
        db.commit()
        return count

    def reindex_document(self, db: Session, doc: KnowledgeDocument) -> int:
        db.query(KnowledgeChunk).filter(KnowledgeChunk.document_id == doc.id).delete()
        chunks = semantic_chunk_text(doc.content)
        for idx, chunk_text in enumerate(chunks):
            emb = compute_hashed_embedding(f"{doc.title} {chunk_text}")
            db.add(
                KnowledgeChunk(
                    document_id=doc.id,
                    chunk_index=idx,
                    content=chunk_text,
                    language=detect_language(chunk_text),
                    embedding_json=emb,
                )
            )
        return len(chunks)

    def retrieve(
        self,
        db: Session,
        query: str,
        top_k: int = 4,
        mode: str = "hybrid",
    ) -> dict[str, Any]:
        self.ingest_default_if_empty(db)
        rows = (
            db.query(KnowledgeChunk, KnowledgeDocument)
            .join(KnowledgeDocument, KnowledgeChunk.document_id == KnowledgeDocument.id)
            .filter(KnowledgeDocument.is_active.is_(True))
            .all()
        )
        if not rows:
            return {"results": [], "grounded": False, "method": mode}

        q_tokens = tokenize(query)
        q_emb = compute_hashed_embedding(query)

        corpus_tokens = [tokenize(f"{doc.title} {chunk.content}") for chunk, doc in rows]
        bm25 = BM25Okapi(corpus_tokens)
        bm25_scores = bm25.get_scores(q_tokens) if q_tokens else [0.0] * len(rows)
        dense_scores = [cosine_similarity(q_emb, chunk.embedding_json or []) for chunk, _ in rows]

        bm25_ranks = {
            idx: rank + 1
            for rank, idx in enumerate(
                sorted(range(len(rows)), key=lambda i: bm25_scores[i], reverse=True)
            )
        }
        dense_ranks = {
            idx: rank + 1
            for rank, idx in enumerate(
                sorted(range(len(rows)), key=lambda i: dense_scores[i], reverse=True)
            )
        }

        scored: list[RetrievedChunk] = []
        for idx, (chunk, doc) in enumerate(rows):
            b_score = float(bm25_scores[idx])
            d_score = float(dense_scores[idx])
            rrf = (1.0 / (60.0 + bm25_ranks[idx])) + (1.0 / (60.0 + dense_ranks[idx]))
            hybrid = round(rrf * 30.0 + max(0.0, b_score) * 0.15 + max(0.0, d_score) * 0.5, 4)
            scored.append(
                RetrievedChunk(
                    doc_id=doc.id,
                    title=doc.title,
                    publisher=doc.publisher,
                    source_url=doc.source_url,
                    last_verified=doc.last_verified,
                    content=chunk.content,
                    bm25_score=round(b_score, 4),
                    dense_score=round(d_score, 4),
                    hybrid_score=hybrid,
                )
            )

        if mode == "bm25":
            scored.sort(key=lambda x: x.bm25_score, reverse=True)
        elif mode == "dense":
            scored.sort(key=lambda x: x.dense_score, reverse=True)
        else:
            scored.sort(key=lambda x: x.hybrid_score, reverse=True)

        top = scored[:top_k]
        # Strict grounding threshold: require strong BM25 term match (> 0.9) OR dense semantic overlap (> 0.25) with BM25 > 0.2
        grounded = bool(top and (top[0].bm25_score > 0.9 or (top[0].dense_score > 0.25 and top[0].bm25_score > 0.2)))
        return {
            "results": top if grounded else [],
            "grounded": grounded,
            "method": mode,
            "top_bm25": top[0].bm25_score if top else 0.0,
            "top_dense": top[0].dense_score if top else 0.0,
        }
