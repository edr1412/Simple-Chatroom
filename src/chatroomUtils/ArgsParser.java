package chatroomUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgsParser {
    public static Map<String, List<String>> parse(String[] args) throws IllegalArgumentException {
        final Map<String, List<String>> params = new HashMap<>();

        List<String> options = null;
        boolean endOption = false;
        for (int i = 0; i < args.length; i++) {
            final String a = args[i];

            if (a.charAt(0) == '-' && !endOption) {
                if (a.length() < 2) {
                    throw new IllegalArgumentException("Error at argument: " + a);
                }
                if(a.equals("--")){
                    endOption=true;
                    continue;
                }
                options = new ArrayList<>();
                if (a.length() > 2 && a.charAt(2)<='9'&&a.charAt(2)>='0'){
                        options.add(a.substring(2));
                        params.put(a.charAt(1)+"", options);
                }
                else if(a.length() > 3 && a.contains("=")&&a.indexOf("=")>1){
                    options.add(a.split("=")[1]);
                    params.put(a.split("=")[0].substring(1), options);
                }
                else{
                    params.put(a.substring(1), options);
                }
            } else if (options != null) {
                options.add(a);
            } else {
                throw new IllegalArgumentException("Illegal parameter usage");
            }
        }
        return params;
    }
}
