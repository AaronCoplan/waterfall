import com.aaroncoplan.waterfall.parser.FileParser;
import org.junit.Assert;
import org.junit.Test;

public class ModuleParsingTests {

    @Test
    public void noWhiteSpace(){
        final String code = "module a{}";
        TestUtils.exec(code);
    }

    @Test
    public void bracketOnNewline() {
        final String code = "module a\n{}";
        TestUtils.exec(code);
    }
}
