# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///

import os
import sys

def create_directory(path):
    os.makedirs(path, exist_ok=True)
    print(f"Created directory: {path}")

def init_concept():
    base_dirs = [
        "_concept/01_concept",
        "_concept/02_features",
        "_concept/03_screens",
        "_concept/05_mockups"
    ]

    for d in base_dirs:
        create_directory(d)

    # Create base index.html mockup template
    index_path = "_concept/05_mockups/index.html"
    try:
        with open(index_path, "w") as f:
            f.write("""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Concept Preview</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://code.iconify.design/iconify-icon/1.0.7/iconify-icon.min.js"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script>
      tailwind.config = {
        theme: {
          extend: {
            fontFamily: {
              sans: ['Inter', 'sans-serif'],
            },
            colors: {
              // Add custom theme colors here
            }
          }
        }
      }
    </script>
</head>
<body class="bg-slate-50 text-slate-900 font-sans antialiased">
    <div class="min-h-screen flex items-center justify-center">
        <div class="text-center">
            <h1 class="text-4xl font-bold tracking-tight text-slate-900 mb-4">Concept Initialized</h1>
            <p class="text-lg text-slate-600">The mockup generator is ready to begin.</p>
        </div>
    </div>
</body>
</html>
""")
        print(f"Created base mockup template at: {index_path}")
    except Exception as e:
        print(f"Error creating base template: {e}")

    # Set up symlink
    try:
        if not os.path.exists("public"):
            create_directory("public")

        symlink_path = "public/mockups"
        if not os.path.islink(symlink_path):
             os.symlink("../_concept/05_mockups", symlink_path)
             print(f"Created symlink: {symlink_path} -> _concept/05_mockups")
        else:
             print(f"Symlink already exists at {symlink_path}")
    except Exception as e:
         print(f"Notice: Could not automatically create symlink (might require admin rights or Windows developer mode). Error: {e}")
         print("Please manually run: ln -s ../_concept/05_mockups public/mockups")


if __name__ == "__main__":
    print("Initializing concept mockup workspace...")
    init_concept()
    print("Initialization complete.")
