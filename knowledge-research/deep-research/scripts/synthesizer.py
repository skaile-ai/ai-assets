#!/usr/bin/env python3
"""
Document Synthesizer Module
Performs semantic clustering and generates final research reports.
"""

import json
from pathlib import Path
from typing import List, Dict, Any, Optional
from dataclasses import dataclass
from datetime import datetime
from collections import defaultdict

try:
    import numpy as np
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.cluster import KMeans
    from sklearn.metrics.pairwise import cosine_similarity
except ImportError:
    print("Error: Required dependencies not found. Run: pip install -r requirements.txt")
    exit(1)


@dataclass
class Cluster:
    """Represents a semantic cluster of sources."""
    id: int
    topic: str
    sources: List[Dict[str, Any]]
    keywords: List[str]
    summary: str
    coherence_score: float


class DocumentSynthesizer:
    """Synthesizes collected documents into a structured research report."""
    
    def __init__(self, research_dir: Path, min_cluster_size: int = 2):
        self.research_dir = Path(research_dir)
        self.min_cluster_size = min_cluster_size
        self.sources: List[Dict[str, Any]] = []
        self.clusters: List[Cluster] = []
        self.metadata: Dict[str, Any] = {}
        
    def load_sources(self) -> None:
        """Load all sources from the research directory."""
        sources_dir = self.research_dir / "sources"
        
        if not sources_dir.exists():
            raise FileNotFoundError(f"Sources directory not found: {sources_dir}")
        
        source_files = sorted(sources_dir.glob("source-*.json"))
        
        print(f"📂 Loading {len(source_files)} sources from {sources_dir}")
        
        for filepath in source_files:
            with open(filepath, 'r', encoding='utf-8') as f:
                source = json.load(f)
                self.sources.append(source)
        
        print(f"✅ Loaded {len(self.sources)} sources")
    
    def perform_clustering(self, n_clusters: Optional[int] = None) -> None:
        """Perform semantic clustering on sources."""
        if not self.sources:
            raise ValueError("No sources loaded. Call load_sources() first.")
        
        print(f"🧩 Performing semantic clustering...")
        
        # Extract text for clustering
        texts = [s.get('content', '') + ' ' + s.get('title', '') for s in self.sources]
        
        # Determine number of clusters
        if n_clusters is None:
            n_clusters = max(2, min(5, len(self.sources) // 3))
        
        # Create TF-IDF vectors
        vectorizer = TfidfVectorizer(
            max_features=100,
            stop_words='english',
            ngram_range=(1, 2)
        )
        
        try:
            vectors = vectorizer.fit_transform(texts)
        except ValueError:
            print("⚠️  Warning: Not enough sources for clustering, creating single cluster")
            self._create_single_cluster()
            return
        
        # Perform K-Means clustering
        kmeans = KMeans(n_clusters=n_clusters, random_state=42, n_init=10)
        labels = kmeans.fit_predict(vectors)
        
        # Group sources by cluster
        cluster_groups = defaultdict(list)
        for idx, label in enumerate(labels):
            cluster_groups[label].append(self.sources[idx])
        
        # Create cluster objects
        feature_names = vectorizer.get_feature_names_out()
        
        for cluster_id, sources in cluster_groups.items():
            # Get top keywords for this cluster
            cluster_center = kmeans.cluster_centers_[cluster_id]
            top_indices = cluster_center.argsort()[-5:][::-1]
            keywords = [feature_names[i] for i in top_indices]
            
            # Calculate coherence (simplified - average pairwise similarity)
            cluster_texts = [s.get('content', '') for s in sources]
            if len(cluster_texts) > 1:
                cluster_vectors = vectorizer.transform(cluster_texts)
                similarities = cosine_similarity(cluster_vectors)
                coherence = float(np.mean(similarities))
            else:
                coherence = 1.0
            
            # Generate topic name
            topic = self._generate_topic_name(keywords, sources)
            
            # Generate cluster summary
            summary = self._generate_cluster_summary(sources, keywords)
            
            cluster = Cluster(
                id=cluster_id,
                topic=topic,
                sources=sources,
                keywords=keywords,
                summary=summary,
                coherence_score=coherence
            )
            
            self.clusters.append(cluster)
        
        # Sort clusters by size (descending)
        self.clusters.sort(key=lambda c: len(c.sources), reverse=True)
        
        print(f"✅ Created {len(self.clusters)} clusters")
        for cluster in self.clusters:
            print(f"   - {cluster.topic}: {len(cluster.sources)} sources")
    
    def _create_single_cluster(self) -> None:
        """Create a single cluster with all sources."""
        cluster = Cluster(
            id=0,
            topic="Research Findings",
            sources=self.sources,
            keywords=["research", "findings"],
            summary="All collected research sources",
            coherence_score=1.0
        )
        self.clusters = [cluster]
    
    def _generate_topic_name(self, keywords: List[str], sources: List[Dict]) -> str:
        """Generate a human-readable topic name from keywords."""
        # Use top 2 keywords
        return " & ".join(keywords[:2]).title()
    
    def _generate_cluster_summary(self, sources: List[Dict], keywords: List[str]) -> str:
        """Generate a summary for a cluster."""
        source_count = len(sources)
        avg_credibility = sum(s.get('credibility_score', 0.5) for s in sources) / source_count
        
        return (f"This cluster contains {source_count} sources "
                f"with an average credibility of {avg_credibility:.2f}. "
                f"Key topics: {', '.join(keywords[:3])}.")
    
    def save_clusters(self) -> None:
        """Save clustering results to JSON."""
        output_file = self.research_dir / "clusters.json"
        
        clusters_data = []
        for cluster in self.clusters:
            clusters_data.append({
                "id": int(cluster.id),
                "topic": cluster.topic,
                "keywords": cluster.keywords,
                "summary": cluster.summary,
                "coherence_score": float(cluster.coherence_score),
                "source_count": len(cluster.sources),
                "source_ids": [s['id'] for s in cluster.sources]
            })
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(clusters_data, f, indent=2, ensure_ascii=False)
        
        print(f"💾 Clusters saved to {output_file}")
    
    def generate_report(self) -> str:
        """Generate final markdown research report."""
        print(f"📝 Generating final research report...")
        
        # Load metadata if exists
        metadata_file = self.research_dir / "metadata.json"
        if metadata_file.exists():
            with open(metadata_file, 'r') as f:
                self.metadata = json.load(f)
        
        query = self.metadata.get('query', 'Research Query')
        timestamp = self.metadata.get('timestamp', datetime.utcnow().isoformat())
        
        report = []
        
        # Header
        report.append(f"# Research Report: {query}")
        report.append(f"\n*Generated: {timestamp}*\n")
        report.append("---\n")
        
        # Executive Summary
        report.append("## Executive Summary\n")
        report.append(f"This report presents findings from **{len(self.sources)} sources** ")
        report.append(f"organized into **{len(self.clusters)} thematic clusters**.\n")
        
        avg_credibility = sum(s.get('credibility_score', 0) for s in self.sources) / len(self.sources)
        report.append(f"- Average source credibility: **{avg_credibility:.2f}**\n")
        report.append(f"- Source types: {', '.join(set(s.get('source_type', 'unknown') for s in self.sources))}\n")
        report.append("\n")
        
        # Research Overview
        report.append("## Research Overview\n")
        report.append(f"**Query:** {query}\n\n")
        report.append(f"**Methodology:** Parallel document collection with semantic clustering\n\n")
        report.append(f"**Sources Analyzed:** {len(self.sources)}\n\n")
        
        # Findings by Cluster
        report.append("## Findings\n")
        
        for cluster in self.clusters:
            report.append(f"### {cluster.topic}\n")
            report.append(f"*{cluster.summary}*\n\n")
            
            report.append(f"**Key Insights:**\n\n")
            
            # Extract insights from top sources in cluster
            top_sources = sorted(
                cluster.sources, 
                key=lambda s: s.get('credibility_score', 0),
                reverse=True
            )[:3]
            
            for idx, source in enumerate(top_sources, 1):
                report.append(f"{idx}. {source.get('summary', 'N/A')} ")
                report.append(f"[[{idx}]](#ref-{source['id']})\n")
            
            report.append("\n")
        
        # Source Analysis
        report.append("## Source Quality Analysis\n")
        
        high_cred = [s for s in self.sources if s.get('credibility_score', 0) >= 0.8]
        med_cred = [s for s in self.sources if 0.6 <= s.get('credibility_score', 0) < 0.8]
        low_cred = [s for s in self.sources if s.get('credibility_score', 0) < 0.6]
        
        report.append(f"- **High credibility** (≥0.8): {len(high_cred)} sources\n")
        report.append(f"- **Medium credibility** (0.6-0.8): {len(med_cred)} sources\n")
        report.append(f"- **Low credibility** (<0.6): {len(low_cred)} sources\n\n")
        
        # Conclusions
        report.append("## Conclusions\n")
        report.append(f"Based on the analysis of {len(self.sources)} sources, ")
        report.append(f"we identified {len(self.clusters)} key thematic areas: ")
        report.append(", ".join([c.topic for c in self.clusters]) + ".\n\n")
        
        # References
        report.append("## References\n")
        
        for idx, source in enumerate(self.sources, 1):
            report.append(f"<a name=\"ref-{source['id']}\"></a>\n")
            report.append(f"[{idx}] **{source.get('title', 'Untitled')}**  \n")
            report.append(f"*{source.get('source_type', 'unknown').title()}* | ")
            report.append(f"Credibility: {source.get('credibility_score', 0):.2f}  \n")
            report.append(f"URL: {source.get('url', 'N/A')}  \n")
            report.append(f"Collected: {source.get('collected_at', 'N/A')}\n\n")
        
        return "".join(report)
    
    def save_report(self, report: str) -> None:
        """Save the final report to markdown file."""
        output_file = self.research_dir / "final-report.md"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(report)
        
        print(f"✅ Final report saved to {output_file}")
    
    def synthesize(self) -> None:
        """Run the complete synthesis workflow."""
        self.load_sources()
        self.perform_clustering()
        self.save_clusters()
        report = self.generate_report()
        self.save_report(report)


def main():
    """Test the synthesizer module."""
    import sys
    
    if len(sys.argv) < 2:
        print("Usage: python synthesizer.py <research_directory>")
        sys.exit(1)
    
    research_dir = Path(sys.argv[1])
    
    if not research_dir.exists():
        print(f"Error: Directory not found: {research_dir}")
        sys.exit(1)
    
    synthesizer = DocumentSynthesizer(research_dir)
    synthesizer.synthesize()
    
    print("\n✅ Synthesis complete!")


if __name__ == "__main__":
    main()
