package ch.heigvd;

import ch.heigvd.commands.ClientCommand;
import ch.heigvd.commands.ServerCommand;
import java.io.File;
import picocli.CommandLine;

/**
 * Main entry point for the DAI NNLS client/server application.
 * Manages global CLI options and dispatches subcommands.
 */
@CommandLine.Command(
        name = "nnls",
        description = "NNLS (Not Live Share) is a client-server network application providing collaborative text file synchronization across multiple users organized into virtual rooms.",
        version = "1.0.0",
        showDefaultValues = true,
        subcommands = {
                ClientCommand.class,
                ServerCommand.class
        },
        mixinStandardHelpOptions = true
)
public class Main {

  /**
   * Main application entry point.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    String jarFilename =
            new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
                    .getName();

    int exitCode = new CommandLine(new Main()).setCommandName(jarFilename).execute(args);

    System.exit(exitCode);
  }
}
