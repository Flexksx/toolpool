{...}: {
  perSystem = {
    pkgs,
    config,
    ...
  }: {
    config.shellPackages = with pkgs; [jdk25 gradle_9 google-java-format];
  };
}
