package ch.heigvd.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "server",
    description = "This command configures and launches the NNLS server on a given port with a given number of threads.",
    mixinStandardHelpOptions = true
)
public class ServerCommand extends BaseCommand {

  static final private int MIN_SERVER_PORT = 1024;
  static final private int MAX_THREADS = 50;  //TODO: find more appropriate number for max threads

  @CommandLine.Option(
          names = {"-t", "--threads"},
          defaultValue = "10",
          description = "Maximum number of threads/rooms in the server at a time."
  )
  protected int nbThreads;

  @Override
  protected void validate(){
    if (port < MIN_SERVER_PORT) {
      throw new IllegalArgumentException(
              "Server Port must be above "+MIN_SERVER_PORT+", got: " + port
      );
    }

    if (nbThreads < 1 || nbThreads > MAX_THREADS){
      throw new IllegalArgumentException(
              "Number of threads must be between 1 and "+MAX_THREADS+", got: " + nbThreads
      );
    }
  }

  @Override
  protected Integer execute() throws Exception {
    return 0;
  }
}
