package ch.heigvd.commands;

import ch.heigvd.Main;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "command2",
    description = "Description for command 2.",
    mixinStandardHelpOptions = true
)
public class CommandTwo implements Callable<Integer> {

  @ParentCommand
  protected Main parent;

  @Override
  public Integer call() {
    // TODO: Implement command 2 logic
    System.out.println("Executing command2...");
    return 0;
  }
}
