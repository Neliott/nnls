package ch.heigvd.commands;

import picocli.CommandLine.Command;

@Command(
        name = "client",
        description = "This command connects the application to the server, using given host and port, and launches the NNLS client REPL.",
    mixinStandardHelpOptions = true
)
public class ClientCommand extends BaseCommand {

  @Override
  protected Integer execute() throws Exception {
    return 0;
  }
}
