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

import de.l9g.crypto.core.CryptoHandler;
import de.l9g.crypto.core.PasswordGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Entry point of the cardinfo service.
 * <p>
 * Besides starting the service, the command line options {@code -e},
 * {@code -g}, {@code -i} and {@code -h} provide the encryption tools for the
 * configuration.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
@SpringBootApplication(exclude =
{
  UserDetailsServiceAutoConfiguration.class
})
public class Application
{

  /**
   * Runs a command line option, or starts the service.
   *
   * @param args {@code -e <clear text>}, {@code -g}, {@code -i}, {@code -h}
   * or Spring Boot arguments
   */
  public static void main(String[] args)
  {
    if(args != null)
    {
      // the secret is loaded (or created) only by the options that need it
      if(args.length == 2 && "-e".equals(args[0]))
      {
        System.out.println(args[1] + " = \"" + CryptoHandler.getInstance().encrypt(args[1]) + "\"");
        System.exit(0);
      }

      if(args.length == 1 && "-g".equals(args[0]))
      {
        String token = PasswordGenerator.generate(32);
        System.out.println("\"" + token + "\" = \"" + CryptoHandler.getInstance().encrypt(token) + "\"");
        System.exit(0);
      }
      
      if(args.length == 1 && "-i".equals(args[0]))
      {
        CryptoHandler.getInstance().encrypt("init");
        log.info("Initialize data/secret.bin");
        System.exit(0);
      }

      if(args.length == 1 && "-h".equals(args[0]))
      {
        System.out.println("l9g-cardinfo [-e clear text] [-g] [-i] [-h]");
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
