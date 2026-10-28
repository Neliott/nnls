package ch.heigvd.commands;

import ch.heigvd.Main;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "command1",
    description = "Description for command 1.",
    mixinStandardHelpOptions = true
)
public class CommandOne implements Callable<Integer> {

  @ParentCommand
  protected Main parent;

  @Override
  public Integer call() {
    // TODO: Implement command 1 logic
    System.out.println("Executing command1...");
    return 0;
  }
}
