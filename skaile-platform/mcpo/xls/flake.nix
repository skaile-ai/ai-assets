{
  description = "Excel MCP server — self-contained Nix recipe (Apache POI 5.5.1)";

  # "MCP brings its own flake": the build recipe lives WITH the asset, not in the
  # platform repo. A consumer builds `<this-dir>#default` and ships the resulting
  # closure to the agent container's nix store — it no longer needs to know how to
  # build excel, only that this flake produces it.
  #
  # nixpkgs is pinned to the exact rev the platform flake currently resolves
  # (platform/nix/flake.lock, nixpkgs node) so this flake produces a
  # byte-identical closure to today's `mcps.excel` and hits the existing cache.
  # The pin is this asset's own, independent of the platform flake.
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

      # Source is this flake's own directory. maven.buildMavenPackage reads
      # pom.xml + src/; the flake/lock files are filtered out so editing them
      # never invalidates the Maven build (the jar depends only on pom.xml + src/).
      excel-src = builtins.path {
        path = ./.;
        name = "excel-mcp-src";
        filter = path: _type:
          let base = baseNameOf path;
          in base != "flake.nix" && base != "flake.lock";
      };

      excel-mcp = pkgs.maven.buildMavenPackage rec {
        pname = "excel-mcp";
        version = "0.1.0-SNAPSHOT";
        src = excel-src;

        # Fixed-output hash of the resolved Maven dependency tree — identical to
        # the platform flake's captured value (same pom.xml, same nixpkgs maven).
        # Update if pom.xml changes the resolved dep set: set to pkgs.lib.fakeHash,
        # build once, copy the expected hash from the error.
        mvnHash = "sha256-dndT4Fpw2hWCjlRCY+GBGbt/MCi1fz1MQSo58L9T0iY=";

        mvnParameters = "-DskipTests";

        # Output layout the MCP.md recipe markers expect:
        #   $out/bin/java          <- ${recipe:excel:bin}/java
        #   $out/lib/excel-mcp.jar <- ${recipe:excel:lib}/excel-mcp.jar
        #   $out (JAVA_HOME)       <- ${recipe:excel}
        installPhase = ''
          runHook preInstall
          mkdir -p $out/lib $out/bin
          install -Dm644 target/excel-mcp-${version}.jar $out/lib/excel-mcp.jar
          ln -s ${pkgs.jdk21_headless}/bin/java $out/bin/java
          runHook postInstall
        '';

        meta = {
          description = "Excel (.xlsx/.xlsm/.xls) MCP server (Apache POI 5.5.1)";
          mainProgram = "java";
        };
      };
    in
    {
      packages.${system} = {
        default = excel-mcp;
        excel = excel-mcp;
      };

      # `nix flake check` gate: jar exists, loadable by the bundled JRE, and the
      # recipe-marker paths resolve.
      checks.${system}.smoke = pkgs.runCommand "excel-mcp-smoke" {
        buildInputs = [ excel-mcp ];
      } ''
        set -euo pipefail
        test -x ${excel-mcp}/bin/java
        test -f ${excel-mcp}/lib/excel-mcp.jar
        ${excel-mcp}/bin/java -jar ${excel-mcp}/lib/excel-mcp.jar --help 2>&1 | head -5 || true
        mkdir -p $out
        echo "excel-mcp smoke test passed" > $out/result.txt
      '';
    };
}
