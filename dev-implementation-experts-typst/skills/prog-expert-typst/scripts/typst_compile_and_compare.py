# /// script
# requires-python = ">=3.11"
# dependencies = [
#     "typer",
#     "pillow",
#     "rich",
# ]
# ///

import subprocess
import sys
import tempfile
from pathlib import Path
import typer
from rich.console import Console
from rich.panel import Panel
from PIL import Image, ImageChops, ImageStat

app = typer.Typer(help="Compile Typst template and compare it with a reference image or PDF.")
console = Console()

def convert_pdf_to_png(pdf_path: Path, output_prefix: str) -> Path:
    """Convert the first page of a PDF to a PNG using pdftoppm."""
    console.print(f"[cyan]Converting reference PDF {pdf_path.name} to PNG...[/cyan]")
    try:
        # pdftoppm -png -f 1 -l 1 <pdf> <prefix>
        # Output will be <prefix>-1.png
        subprocess.run(
            ["pdftoppm", "-png", "-f", "1", "-l", "1", str(pdf_path), output_prefix],
            check=True,
            capture_output=True
        )
        expected_output = Path(f"{output_prefix}-1.png")
        if not expected_output.exists():
            # Sometimes it might just output <prefix>.png depending on version/single page
            alt_output = Path(f"{output_prefix}.png")
            if alt_output.exists():
                return alt_output
            console.print("[red]Failed to find pdftoppm output![/red]")
            raise typer.Exit(1)
        return expected_output
    except FileNotFoundError:
        console.print("[red]pdftoppm not found! Please ensure poppler-utils is installed.[/red]")
        raise typer.Exit(1)
    except subprocess.CalledProcessError as e:
        console.print(f"[red]pdftoppm failed:[/red] {e.stderr.decode()}")
        raise typer.Exit(1)

def compute_difference(img1_path: Path, img2_path: Path, diff_out: Path) -> float:
    """Compute structural difference and save a diff image."""
    try:
        with Image.open(img1_path) as i1, Image.open(img2_path) as i2:
            # Ensure same size and mode
            if i1.size != i2.size:
                console.print(f"[yellow]Warning: Image sizes differ ({i1.size} vs {i2.size}). Resizing compiled image to match reference.[/yellow]")
                i1 = i1.resize(i2.size, Image.Resampling.LANCZOS)
            
            i1 = i1.convert("RGB")
            i2 = i2.convert("RGB")

            diff = ImageChops.difference(i1, i2)
            diff.save(diff_out)
            
            stat = ImageStat.Stat(diff)
            # Calculate RMS diff
            diff_ratio = sum(stat.rms) / (len(stat.rms) * 255.0)
            return diff_ratio * 100.0
    except Exception as e:
        console.print(f"[red]Failed to compare images: {e}[/red]")
        raise typer.Exit(1)

@app.command()
def verify(
    typst_file: Path = typer.Argument(..., help="Path to the main.typ file"),
    reference_file: Path = typer.Argument(..., help="Path to the reference PDF or Image"),
    output_image: Path = typer.Option(Path("compiled_output.png"), "--output", "-o", help="Path to save the compiled Typst image"),
    diff_image: Path = typer.Option(Path("diff_output.png"), "--diff", "-d", help="Path to save the difference map"),
):
    """Compile Typst and compare with reference."""
    if not typst_file.exists():
        console.print(f"[red]Typst file {typst_file} not found![/red]")
        raise typer.Exit(1)
        
    if not reference_file.exists():
        console.print(f"[red]Reference file {reference_file} not found![/red]")
        raise typer.Exit(1)

    # 1. Compile Typst
    console.print(f"[cyan]Compiling {typst_file.name}...[/cyan]")
    try:
        # We compile to a single PNG page (assuming single page posters/templates)
        # Using typst compile <file> <output>
        subprocess.run(
            ["typst", "compile", str(typst_file), str(output_image)],
            check=True,
            capture_output=True,
            text=True
        )
        console.print(f"[green]Successfully compiled to {output_image}[/green]")
    except FileNotFoundError:
        console.print("[red]typst CLI not found![/red]")
        raise typer.Exit(1)
    except subprocess.CalledProcessError as e:
        console.print(Panel(e.stderr, title="[red]Typst Compilation Error[/red]", border_style="red"))
        raise typer.Exit(1)

    # Handle multi-page typst output e.g. compiled_output-1.png
    actual_compiled = output_image
    if not output_image.exists():
        stem = output_image.stem
        ext = output_image.suffix
        expected_multi = output_image.parent / f"{stem}-1{ext}"
        if expected_multi.exists():
             actual_compiled = expected_multi
        else:
             console.print(f"[red]Could not find compiled output at {output_image}[/red]")
             raise typer.Exit(1)

    reference_png_path = reference_file

    with tempfile.TemporaryDirectory() as tmpdir:
        tmp_path = Path(tmpdir)
        
        # 2. Convert reference to PNG if PDF
        if reference_file.suffix.lower() == ".pdf":
            reference_png_path = convert_pdf_to_png(reference_file, str(tmp_path / "ref"))
        
        # 3. Compare images
        console.print(f"[cyan]Comparing compiled output with {reference_file.name}...[/cyan]")
        diff_score = compute_difference(actual_compiled, reference_png_path, diff_image)
        
        console.print(Panel(
            f"[bold]Difference Score:[/bold] {diff_score:.2f}%\n"
            f"[dim]0% means identical. Lower is better.[/dim]\n\n"
            f"[bold]Diff Map Saved To:[/bold] {diff_image}",
            title="[green]Comparison Complete[/green]",
            border_style="green"
        ))

if __name__ == "__main__":
    app()
