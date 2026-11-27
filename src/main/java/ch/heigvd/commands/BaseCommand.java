package ch.heigvd.commands;

import picocli.CommandLine;
import java.util.concurrent.Callable;

/**
 * Base class for all CLI subcommands, implementing the Template Method pattern
 * for standardized error, validation, and exception handling across commands.
 */
public abstract class BaseCommand implements Callable<Integer> {

  static final private int MIN_PORT = 1;
  static final private int MAX_PORT = 65535;

  @CommandLine.Option(
          names = {"-p", "--port"},
          defaultValue = "4242",
          description = "NNLS Server port to connect to as a client and to expose as the server."
  )
  protected int port;

  @Override
  public Integer call() {
    int exitCode = 1;
    try {
      validateCommon();
      validate();
      exitCode = execute();
    } catch (IllegalArgumentException e) {
      System.err.println("Error: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("Error: Unexpected error: " + e.getMessage());
    }

    return exitCode;
  }

  /**
   * Validates common command parameter, ensuring port is not incoherent
   *
   * @throws IllegalArgumentException if validation constraints are violated
   */
  protected void validateCommon() throws IllegalArgumentException {
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException(
              "Port must be between "+MIN_PORT+" and "+MAX_PORT+", got: " + port
      );
    }
  }

  /**
   * Hook for subcommands to perform specific validations before execution.
   *
   * @throws Exception if subcommand-specific validation fails
   */
  protected void validate() throws Exception {
    // Default implementation does nothing
  }

  /**
   * Executes the command logic. Subclasses implement this method without needing
   * to catch exceptions.
   *
   * @return exit code (0 for success, non-zero for error)
   * @throws Exception if an error occurs during execution
   */
  protected abstract Integer execute() throws Exception;
}
