#!/usr/bin/env python3
"""
Deep Research Orchestrator
Main entry point for the integrated deep research workflow.
"""

import argparse
import asyncio
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from collector import DocumentCollector
from synthesizer import DocumentSynthesizer


class DeepResearcher:
    """Orchestrates the complete deep research workflow."""
    
    def __init__(
        self,
        query: str,
        output_dir: str = "./research-output",
        max_sources: int = 15,
        max_parallel: int = 3,
        source_types: Optional[list] = None,
        min_credibility: float = 0.6,
        no_synthesis: bool = False
    ):
        self.query = query
        self.output_dir = Path(output_dir)
        self.max_sources = max_sources
        self.max_parallel = max_parallel
        self.source_types = source_types
        self.min_credibility = min_credibility
        self.no_synthesis = no_synthesis
        
        # Create topic slug for directory name
        self.topic_slug = self._create_slug(query)
        self.timestamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        
        # Research directory
        self.research_dir = self.output_dir / self.topic_slug / self.timestamp
        
        # Metadata
        self.metadata = {
            "query": query,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "sources_collected": 0,
            "sources_used": 0,
            "clusters_identified": 0,
            "synthesis_completed": False,
            "duration_seconds": 0.0
        }
    
    def _create_slug(self, text: str) -> str:
        """Create URL-safe slug from text."""
        import re
        slug = text.lower()
        slug = re.sub(r'[^\w\s-]', '', slug)
        slug = re.sub(r'[-\s]+', '-', slug)
        return slug[:50]  # Limit length
    
    async def execute(self) -> dict:
        """Execute the complete research workflow."""
        start_time = datetime.now(timezone.utc)
        
        print("\n" + "="*70)
        print("🔬 DEEP RESEARCH - Integrated Research Workflow")
        print("="*70)
        print(f"\n📋 Query: {self.query}")
        print(f"📁 Output: {self.research_dir.resolve()}")
        print(f"🎯 Target: {self.max_sources} sources\n")
        
        try:
            # Phase 1: Collection
            await self.collect()
            
            # Phase 2: Synthesis (unless disabled)
            if not self.no_synthesis:
                self.synthesize()
            else:
                print("\n⏭️  Skipping synthesis (--no-synthesis flag set)")
            
            # Calculate duration
            end_time = datetime.now(timezone.utc)
            duration = (end_time - start_time).total_seconds()
            self.metadata["duration_seconds"] = duration
            
            # Save final metadata
            self._save_metadata()
            
            print("\n" + "="*70)
            print("✅ RESEARCH COMPLETE")
            print("="*70)
            print(f"⏱️  Duration: {duration:.1f} seconds")
            print(f"📊 Sources collected: {self.metadata['sources_collected']}")
            if self.metadata["synthesis_completed"]:
                print(f"🧩 Clusters identified: {self.metadata['clusters_identified']}")
                print(f"📝 Final report: {self.research_dir / 'final-report.md'}")
            print(f"📁 Full results: {self.research_dir}\n")
            
            return self.metadata
            
        except KeyboardInterrupt:
            print("\n\n⚠️  Research cancelled by user")
            sys.exit(130)
        except Exception as e:
            print(f"\n\n❌ Error during research: {e}")
            import traceback
            traceback.print_exc()
            sys.exit(1)
    
    async def collect(self) -> None:
        """Execute document collection phase."""
        print("="*70)
        print("PHASE 1: DOCUMENT COLLECTION")
        print("="*70 + "\n")
        
        # Create research directory
        self.research_dir.mkdir(parents=True, exist_ok=True)
        
        # Initialize collector
        collector = DocumentCollector(
            query=self.query,
            max_sources=self.max_sources,
            max_parallel=self.max_parallel,
            source_types=self.source_types,
            min_credibility=self.min_credibility
        )
        
        # Collect documents
        sources = await collector.collect()
        
        # Save sources
        collector.save_sources(self.research_dir)
        
        # Update metadata
        self.metadata["sources_collected"] = len(sources)
        self.metadata["sources_used"] = len(sources)
        
        print(f"\n✅ Collection phase complete\n")
    
    def synthesize(self) -> None:
        """Execute document synthesis phase."""
        print("="*70)
        print("PHASE 2: DOCUMENT SYNTHESIS")
        print("="*70 + "\n")
        
        # Initialize synthesizer
        synthesizer = DocumentSynthesizer(
            research_dir=self.research_dir,
            min_cluster_size=2
        )
        
        # Run synthesis
        synthesizer.synthesize()
        
        # Update metadata
        self.metadata["clusters_identified"] = len(synthesizer.clusters)
        self.metadata["synthesis_completed"] = True
        
        print(f"\n✅ Synthesis phase complete\n")
    
    def _save_metadata(self) -> None:
        """Save research metadata."""
        metadata_file = self.research_dir / "metadata.json"
        
        with open(metadata_file, 'w', encoding='utf-8') as f:
            json.dump(self.metadata, f, indent=2, ensure_ascii=False)
        
        print(f"💾 Metadata saved to {metadata_file}")


def main():
    """Main entry point for CLI."""
    parser = argparse.ArgumentParser(
        description="Deep Research - Integrated research workflow with collection, storage, and synthesis",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Basic research
  python research.py --query "Kubernetes history"
  
  # Custom output directory
  python research.py --query "Python web frameworks" --output ./my-research
  
  # More sources with custom types
  python research.py --query "Machine learning trends" --max-sources 20 --source-types academic,blog
  
  # Collection only (no synthesis)
  python research.py --query "Docker vs Podman" --no-synthesis
        """
    )
    
    parser.add_argument(
        "--query", "-q",
        required=True,
        help="Research query to investigate"
    )
    
    parser.add_argument(
        "--output", "-o",
        default="./research-output",
        help="Output directory for research results (default: ./research-output)"
    )
    
    parser.add_argument(
        "--max-sources",
        type=int,
        default=15,
        help="Maximum number of sources to collect (default: 15)"
    )
    
    parser.add_argument(
        "--max-parallel",
        type=int,
        default=3,
        help="Maximum parallel collection tasks (default: 3)"
    )
    
    parser.add_argument(
        "--source-types",
        help="Comma-separated list of source types (e.g., academic,blog,documentation)"
    )
    
    parser.add_argument(
        "--min-credibility",
        type=float,
        default=0.6,
        help="Minimum credibility score for sources (default: 0.6)"
    )
    
    parser.add_argument(
        "--no-synthesis",
        action="store_true",
        help="Skip synthesis phase (collection only)"
    )
    
    args = parser.parse_args()
    
    # Parse source types
    source_types = None
    if args.source_types:
        source_types = [s.strip() for s in args.source_types.split(',')]
    
    # Create researcher
    researcher = DeepResearcher(
        query=args.query,
        output_dir=args.output,
        max_sources=args.max_sources,
        max_parallel=args.max_parallel,
        source_types=source_types,
        min_credibility=args.min_credibility,
        no_synthesis=args.no_synthesis
    )
    
    # Execute research
    asyncio.run(researcher.execute())


if __name__ == "__main__":
    main()
