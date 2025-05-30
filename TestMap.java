// TestMap.java
import java.util.Map;
import java.util.HashMap;
public class TestMap {
    public static void main(String[] args) {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("test", "value");
        System.out.println(myMap.get("test"));
    }
}
