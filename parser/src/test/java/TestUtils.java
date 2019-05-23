import com.aaroncoplan.waterfall.parser.FileParser;
import com.aaroncoplan.waterfall.parser.ParseResult;
import org.junit.Assert;

public class TestUtils {

    public static void exec(String code) {
        final ParseResult result = FileParser.parseCodeString(null, code);
        if(result.hasErrors()) {
            result.getSyntaxErrors().forEach(System.out::println);
        }
        Assert.assertFalse(result.hasErrors());
    }
}
