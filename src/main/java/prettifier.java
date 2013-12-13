import org.markdown4j.Markdown4jProcessor;
import org.markdown4j.Plugin;
import prettify.PrettifyParser;
import syntaxhighlight.ParseResult;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

class CodeHighlight extends Plugin {
  public CodeHighlight() {
    super("code");
  }

  @Override
  public void emit(StringBuilder out, List<String> lines, Map<String, String> params) {
    String lang = params.get("lang");
    if (lang == null)
      lang = "scala";     // defaults to Scala, change as to your favorite lang ;)
    String content = "";
    for (String line : lines)
      content += line;

    PrettifyParser parser = new PrettifyParser();
    out.append("<pre class=\"prettyprint prettyprinted\" style>\n");
    for (ParseResult pr : parser.parse(lang, content)) {
      out.append("  <span class=\"" + pr.getStyleKeys().get(0) + "\">"
        + content.substring(pr.getOffset(), pr.getOffset()+pr.getLength()) + "</span>\n");
    }
    out.append("</pre>\n");
  }
}