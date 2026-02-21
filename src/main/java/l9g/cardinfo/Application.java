/*
 * Copyright 2026 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l9g.cardinfo;

import l9g.cardinfo.crypto.CryptoHandler;
import l9g.cardinfo.crypto.PasswordGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@Slf4j
@SpringBootApplication(exclude =
{
  UserDetailsServiceAutoConfiguration.class
})
public class Application
{

  public static void main(String[] args)
  {
    if(args != null)
    {
      CryptoHandler cryptoHandler = CryptoHandler.getInstance();

      if(args.length == 2 && "-e".equals(args[0]))
      {
        System.out.println(args[1] + " = \"" + cryptoHandler.encrypt(args[1]) + "\"");
        System.exit(0);
      }

      if(args.length == 1 && "-g".equals(args[0]))
      {
        String token = PasswordGenerator.generate(32);
        System.out.println("\"" + token + "\" = \"" + cryptoHandler.encrypt(token) + "\"");
        System.exit(0);
      }
      
      if(args.length == 1 && "-i".equals(args[0]))
      {
        cryptoHandler.encrypt("init");
        log.info("Initialize data/secret.bin");
        System.exit(0);
      }

      if(args.length == 1 && "-h".equals(args[0]))
      {
        System.out.println("l9g-uidgen [-e clear text] [-g] [-h]");
        System.out.println("  -e : encrypt clear text");
        System.out.println("  -g : generate new token");
        System.out.println("  -i : initialize data/secret.bin");
        System.out.println("  -h : this help");
        System.exit(0);
      }
    }

    SpringApplication.run(Application.class, args);
  }

}
