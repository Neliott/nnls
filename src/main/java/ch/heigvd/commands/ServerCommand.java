package ch.heigvd.commands;

import picocli.CommandLine.Command;

@Command(
    name = "server",
    description = "This command configures and launches the NNLS server on a given port with a given number of threads.",
    mixinStandardHelpOptions = true
)
public class ServerCommand extends BaseCommand {

  static final private int MIN_SERVER_PORT = 1024;

  @Override
  protected void validate(){
    if (port < MIN_SERVER_PORT) {
      throw new IllegalArgumentException(
              "Server Port must be above "+MIN_SERVER_PORT+", got: " + port
      );
    }
  }

  @Override
  protected Integer execute() throws Exception {
    return 0;
  }
}
