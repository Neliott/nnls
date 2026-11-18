package ch.heigvd;

import ch.heigvd.commands.CommandOne;
import ch.heigvd.commands.CommandTwo;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
    name = "nnls",
    description = "Process an input file and write the result to an output file.",
    version = "1.0.0",
    showDefaultValues = true,
    subcommands = {
        CommandOne.class,
        CommandTwo.class
    },
    scope = ScopeType.INHERIT,
    mixinStandardHelpOptions = true
)
public class Main {

  @Option(
      names = {"-i", "--input"},
      description = "The input file."
  )
  protected File inputFile;

  @Option(
      names = {"-I", "--input-encoding"},
      description = "The input file encoding (default: UTF-8)."
  )
  protected Charset inputEncoding = StandardCharsets.UTF_8;

  @Option(
      names = {"-o", "--output"},
      description = "The output file."
  )
  protected File outputFile;

  @Option(
      names = {"-O", "--output-encoding"},
      description = "The output file encoding (default: UTF-8)."
  )
  protected Charset outputEncoding = StandardCharsets.UTF_8;

  public File getInputFile() {
    return inputFile;
  }

  public Charset getInputEncoding() {
    return inputEncoding;
  }

  public File getOutputFile() {
    return outputFile;
  }

  public Charset getOutputEncoding() {
    return outputEncoding;
  }

  public static void main(String[] args) {
    String jarFilename = "nnls.jar";
    try {
      File location = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (location.isFile()) {
        jarFilename = location.getName();
      }
    } catch (Exception ignored) {
      // Fallback to default name
    }

    int exitCode = new CommandLine(new Main())
        .setCommandName(jarFilename)
        .execute(args);

    System.exit(exitCode);
  }
}
