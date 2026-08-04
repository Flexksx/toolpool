{...}: {
  perSystem = {
    pkgs,
    lib,
    system,
    ...
  }: let
    version = "2.4.6";
    sources = {
      x86_64-linux = {
        asset = "moon_cli-x86_64-unknown-linux-gnu.tar.xz";
        sha256 = "106a4b18ddd93e9485a396c14b3a7e287586a006ea201e6a0b37e9e221f51d97";
      };
      aarch64-linux = {
        asset = "moon_cli-aarch64-unknown-linux-gnu.tar.xz";
        sha256 = "5eb8afeae1afca5a74efe8db9aafb1fc47ca5c10a0fe976b740f3ad45b3a5cae";
      };
      x86_64-darwin = {
        asset = "moon_cli-x86_64-apple-darwin.tar.xz";
        sha256 = "31ed209e4ced4df2294407d55f99e38e46353471b3e1992ec5162a32151cceb5";
      };
      aarch64-darwin = {
        asset = "moon_cli-aarch64-apple-darwin.tar.xz";
        sha256 = "5bc863dd2c5e18c11e35a035318e6a3b5aaa7636f6c16ec6bd6366baf031e596";
      };
    };
    source = sources.${system};
    moon = pkgs.stdenv.mkDerivation {
      pname = "moon";
      inherit version;

      src = pkgs.fetchurl {
        url = "https://github.com/moonrepo/moon/releases/download/v${version}/${source.asset}";
        sha256 = source.sha256;
      };

      nativeBuildInputs = lib.optionals pkgs.stdenv.isLinux [pkgs.autoPatchelfHook];
      buildInputs = lib.optionals pkgs.stdenv.isLinux [pkgs.stdenv.cc.cc.lib];

      dontConfigure = true;
      dontBuild = true;

      installPhase = ''
        runHook preInstall
        install -Dm755 moon $out/bin/moon
        runHook postInstall
      '';

      meta = {
        description = "A monorepo build system and task runner, used here to resolve the apps/jopenapi-demo -> libs/jopenapimcp task graph";
        homepage = "https://github.com/moonrepo/moon";
        mainProgram = "moon";
        platforms = ["x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin"];
      };
    };
  in {
    config.shellPackages = [moon];
  };
}
