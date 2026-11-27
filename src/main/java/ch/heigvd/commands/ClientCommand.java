package ch.heigvd.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Command(
        name = "client",
        description = "This command connects the application to the server, using given host and port, and launches the NNLS client REPL.",
    mixinStandardHelpOptions = true
)
public class ClientCommand extends BaseCommand {

  private static final Pattern HOSTNAME_PATTERN =
          Pattern.compile("(?i)^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*$");

  private static boolean isValidIpLiteral(String candidate) {
    try {
      if (candidate.matches("\\d{1,3}(\\.\\d{1,3}){3}") || candidate.contains(":")) {
        InetAddress.getByName(candidate);
        return true;
      }
      return false;
    } catch (UnknownHostException e) {
      return false;
    }
  }

  @CommandLine.Option(
          names = {"-H", "--host"},
          required = true,
          description = "Host name of the NNLS server to connect to."
  )
  protected String host;

  @Override
  protected void validate() {
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Host must not be blank.");
    }

    String normalized = host.trim();
    if (normalized.length() > 253) {
      throw new IllegalArgumentException("Host name is too long: " + host);
    }

    String unbracketed = (normalized.startsWith("[") && normalized.endsWith("]"))
            ? normalized.substring(1, normalized.length() - 1)
            : normalized;

    if (!isValidIpLiteral(unbracketed) && !HOSTNAME_PATTERN.matcher(normalized).matches()) {
      throw new IllegalArgumentException("Invalid host name or IP address: " + host);
    }

    this.host = normalized;
  }

  @Override
  protected Integer execute() throws Exception {

    return 0;
  }
}
