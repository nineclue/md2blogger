import org.markdown4j.Plugin;
import prettify.PrettifyParser;
import syntaxhighlight.ParseResult;
import java.util.Map;
import java.util.List;

class CodeHighlight extends Plugin {
  public CodeHighlight() {
    super("code");
  }

  @Override
  public void emit(StringBuilder out, List<String> lines, Map<String, String> params) {
    String lang = params.get("lang");
    if (lang == null)
      lang = "scala";     // defaults to Scala, change as to your favorite lang ;)

    PrettifyParser parser = new PrettifyParser();
    out.append("<pre class=\"prettyprint prettyprinted\" style>\n");
    for (String line : lines) {
      for (ParseResult pr : parser.parse(lang, line)) {
        out.append("<span class=\"" + pr.getStyleKeys().get(0) + "\">"
          + line.substring(pr.getOffset(), pr.getOffset()+pr.getLength()) + "</span>");
      }
      out.append("\n");
    }
    out.append("</pre>\n");
  }
}