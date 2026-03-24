package com.sendBulkMail.sendBulkMail.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.regex.Pattern;

@Service
@Slf4j
public class EmailValidatorService {

    // Layer 1: Regex for obvious typos
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
            "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Validates an email address using both Regex and MX record check.
     * @param email The email address to validate.
     * @return true if both checks pass, false otherwise.
     */
    public boolean isValid(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Layer 1: Regex Check
        if (!isRegexValid(email)) {
            log.warn("Email failed Regex validation: {}", email);
            return false;
        }

        // Layer 2: MX Check
        if (!hasMxRecord(email)) {
            log.warn("Email failed MX record validation: {}", email);
            return false;
        }

        return true;
    }

    /**
     * Layer 1: Regex validation.
     */
    public boolean isRegexValid(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Layer 2: MX record check.
     */
    public boolean hasMxRecord(String email) {
        String domain = email.substring(email.indexOf('@') + 1);
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[]{"MX"});
            Attribute attr = attrs.get("MX");

            if (attr == null || attr.size() == 0) {
                // If no MX records, check for A record (some mail servers use A record if MX is missing)
                attrs = ictx.getAttributes(domain, new String[]{"A"});
                attr = attrs.get("A");
                if (attr == null || attr.size() == 0) {
                    return false;
                }
            }
            return true;
        } catch (NamingException e) {
            log.debug("No MX or A record found for domain: {}. Error: {}", domain, e.getMessage());
            return false;
        }
    }
}
