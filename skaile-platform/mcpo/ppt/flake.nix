{
  description = "PowerPoint MCP server — self-contained Nix recipe (Apache POI + LibreOffice)";

  # "MCP brings its own flake": the build recipe lives WITH the asset, not in the
  # platform repo. A consumer builds `<this-dir>#default` and ships the resulting
  # closure to the agent container's nix store.
  #
  # nixpkgs is pinned to the exact rev the platform flake resolves
  # (platform/nix/flake.lock, nixpkgs node) so this flake produces a
  # content-equivalent closure to today's `mcps.ppt`.
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/0c88e1f2bdb93d5999019e99cb0e61e1fe2af4c5";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = false;
      };

      ppt-src = builtins.path {
        path = ./.;
        name = "ppt-mcp-src";
        filter = path: _type:
          let base = baseNameOf path;
          in base != "flake.nix" && base != "flake.lock";
      };

      ppt-mcp = pkgs.maven.buildMavenPackage rec {
        pname = "ppt-mcp-server";
        version = "1.0.0";
        src = ppt-src;

        # Fixed-output hash of the resolved Maven dependency tree — identical to
        # the platform flake's captured value (same pom.xml, same nixpkgs maven).
        # Update if pom.xml changes the resolved dep set: set to pkgs.lib.fakeHash,
        # build once, copy the expected hash from the error.
        mvnHash = "sha256-tsYfnlFjC4yTPGHSPfjrV+yCttmBau4nnNPs+iN+O7E=";

        mvnParameters = "-DskipTests";

        # Output layout the MCP.md recipe markers expect:
        #   $out/bin/java     <- ${recipe:ppt:bin}/java
        #   $out/bin/soffice  <- ${recipe:ppt:bin}/soffice
        #   $out/lib/ppt-mcp.jar <- ${recipe:ppt:lib}/ppt-mcp.jar
        #   $out (JAVA_HOME)  <- ${recipe:ppt}
        installPhase = ''
          runHook preInstall
          mkdir -p $out/lib $out/bin
          # maven-shade-plugin finalName = ppt-mcp-server-all.
          install -Dm644 target/ppt-mcp-server-all.jar $out/lib/ppt-mcp.jar
          ln -s ${pkgs.jdk21_headless}/bin/java $out/bin/java
          # MCP.md declares SOFFICE_PATH: ${recipe:ppt:bin}/soffice
          ln -s ${pkgs.libreoffice-still}/bin/soffice $out/bin/soffice
          runHook postInstall
        '';

        meta = {
          description = "PowerPoint (.pptx) MCP server (Apache POI + LibreOffice)";
          mainProgram = "java";
        };
      };
    in
    {
      packages.${system} = {
        default = ppt-mcp;
        ppt = ppt-mcp;
      };

      checks.${system}.smoke = pkgs.runCommand "ppt-mcp-smoke" {
        buildInputs = [ ppt-mcp ];
      } ''
        set -euo pipefail
        test -x ${ppt-mcp}/bin/java
        test -x ${ppt-mcp}/bin/soffice
        test -f ${ppt-mcp}/lib/ppt-mcp.jar
        ${ppt-mcp}/bin/soffice --version
        mkdir -p $out
        echo "ppt-mcp smoke test passed" > $out/result.txt
      '';
    };
}
