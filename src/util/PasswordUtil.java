package util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String createHashPassword(String password){



        return BCrypt.hashpw(password, BCrypt.gensalt(12));

    }


}
