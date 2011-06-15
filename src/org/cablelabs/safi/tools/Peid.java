package org.cablelabs.safi.tools;
/** 
 * <BeginCopyright>  
 * Notice 
 * <p> 
 * This document is the result of a cooperative effort undertaken at the direction of Cable 
 * Television Laboratories, Inc. for the benefit of the cable industry and its customers. This 
 * document may contain references to other documents not owned or controlled by CableLabs. Use 
 * and understanding of this document may require access to such other documents. Designing, 
 * manufacturing, distributing, using, selling, or servicing products, or providing services, based 
 * on this document may require intellectual property licenses from third parties for technology 
 * referenced in this document. Neither CableLabs nor any member company is responsible to any party
 * for any liability of any nature whatsoever resulting from or arising out of use or reliance upon
 * this document, or any document referenced herein. This document is furnished on an "AS IS" basis
 * and neither CableLabs nor its members provides any representation or warranty, express or 
 * implied, regarding the accuracy, completeness, non-infringement, or fitness for a particular 
 * purpose of this document, or any document referenced herein. Distribution of this document is
 * restricted pursuant to the terms of separate access agreements negotiated with each of the 
 * parties to whom this document has been furnished. 
 * <p> 
 * Copyright 2009 Cable Television Laboratories, Inc. All rights reserved. 
 * <EndCopyright>  
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;

import org.w3c.tools.codec.Base64UrlDecoder;
import org.w3c.tools.codec.Base64UrlEncoder;
import org.w3c.tools.codec.Base64FormatException;

/**
 * This is a class that holds a PEID (per RFC 4122) and can accept and return in a variety of forms.
 * They are 1) native, an array of 16 binary bytes, 2) canonical form,  (32 characters
 * consisting of 32 hex digits with dashes at specific places per RFC 4122), 3) the normal 24 byte 
 * Base64 version, and 4) Cablelabs(R) SaFI PEID form - 22 characters of Base64 form per RFC 3548
 * with padding suppressed. 
 * <p> Revision 1 - dvb - May 19, 09 Allow input to be comma-delimited string
 */
public class Peid
{
    private static final int PEID_LENGTH = 16;
    private final static String kHexChars = "0123456789abcdefABCDEF";
    
    private byte[] mPeid;
    boolean loaded = false;
    
    /**
     * Constructor, empty requiring one of the set calls
     */
    public Peid()
    {
        mPeid = new byte[PEID_LENGTH];
        for (int i = 0; i < PEID_LENGTH; i++)
        {
            mPeid[i] = 0;
        }
        loaded = false;
    }


    /* Routine for loading a PEID from the canonical string
     * representation.
     *
     * Note that implementation is optimized for speed, not necessarily
     * code clarity
     *
     * @param id String that contains the canonical representation of
     *   the PEID to build; 36-char string (see UUID specs for details).
     *   Hex-chars may be in upper-case too; UUID class will always output
     *   them in lowercase.
     */
    public byte[] setPeidCanonical(String canonicalId)
        throws NumberFormatException
    {
        if (canonicalId == null) {
            throw new NullPointerException();
        }
        if (canonicalId.length() != 36) {
            throw new NumberFormatException("UUID has to be represented by the standard 36-char representation");
        }

        for (int i = 0, j = 0; i < 36; ++j) {
            // Need to bypass hyphens:
            switch (i) {
            case 8:
            case 13:
            case 18:
            case 23:
                if (canonicalId.charAt(i) != '-') {
                    throw new NumberFormatException("UUID has to be represented by the standard 36-char canonical representation");
                }
                ++i;
            }
            int index;
            char c = canonicalId.charAt(i);

            if (c >= '0' && c <= '9') {
                mPeid[j] = (byte) ((c - '0') << 4);
            } else if (c >= 'a' && c <= 'f') {
                mPeid[j] = (byte) ((c - 'a' + 10) << 4);
            } else if (c >= 'A' && c <= 'F') {
                mPeid[j] = (byte) ((c - 'A' + 10) << 4);
            } else {
                throw new NumberFormatException("Non-hex character '"+c+"'");
            }

            c = canonicalId.charAt(++i);

            if (c >= '0' && c <= '9') {
                mPeid[j] |= (byte) (c - '0');
            } else if (c >= 'a' && c <= 'f') {
                mPeid[j] |= (byte) (c - 'a' + 10);
            } else if (c >= 'A' && c <= 'F') {
                mPeid[j] |= (byte) (c - 'A' + 10);
            } else {
                throw new NumberFormatException("Non-hex character '"+c+"'");
            }
            ++i;
        }
        loaded = true;
        return mPeid;
    }

    /**
     * Routine for loading the UUID from base 64 representation. All Base64 encodings of a PEID end
     * in two '=' signs. 
     */
    public byte[] setPeidBase64(String base64Str)
    throws java.io.IOException, org.w3c.tools.codec.Base64FormatException
    {
        if (base64Str == null) {
            throw new NullPointerException();
        }
        if (base64Str.length() != 24) {
            throw new NumberFormatException("UUID has to be represented by the standard 24-char base64 representation");
        }
        
        StringBufferInputStream sbrdr = new java.io.StringBufferInputStream(base64Str);
        ByteArrayOutputStream wtr = new ByteArrayOutputStream();
        Base64UrlDecoder b = new Base64UrlDecoder (sbrdr,wtr);
        b.setEncodeUrl(false);
        b.setDropPadding(false);
        b.process();
        byte[] tmpPeid = wtr.toByteArray();
        if (tmpPeid.length != PEID_LENGTH)
        {
            throw new NumberFormatException("UUID conversion error, resulting length is "+tmpPeid.length+", must be "+PEID_LENGTH);
        }
        loaded = true;
        mPeid = tmpPeid;
        return mPeid;
    }

    /**
     * Routine for loading a PEID from the modified base 64 URL representation. This encoding
     * uses the normal Base64 char substituions, but has the two trailing '==' removed, as in 
     * Base64URL 
     */
    public byte[] setPeidBase64Unpadded(String base64Str)
    throws java.io.IOException, org.w3c.tools.codec.Base64FormatException
    {
        if (base64Str == null) {
            throw new NullPointerException();
        }
        if (base64Str.length() != 22) {
            throw new NumberFormatException("PEID has to be represented by the modified 22-char base64Url representation");
        }
        
        StringBufferInputStream sbrdr = new java.io.StringBufferInputStream(base64Str);
        ByteArrayOutputStream wtr = new ByteArrayOutputStream();
        Base64UrlDecoder b = new Base64UrlDecoder (sbrdr,wtr);
        b.setEncodeUrl(false);
        b.setDropPadding(true);
        b.process();
        byte[] tmpPeid = wtr.toByteArray();
        //Note we get extra byte from partial
        if (tmpPeid.length != PEID_LENGTH+1)
        {
            throw new NumberFormatException("UUID conversion error, resulting length is "+tmpPeid.length+", must be "+PEID_LENGTH);
        }
        loaded = true;
        mPeid = new byte[PEID_LENGTH];
        System.arraycopy(tmpPeid, 0, mPeid, 0, PEID_LENGTH);
        return mPeid;
    }

    /**
     * Routine for loading the PEID from an existing byte array
     */
    public byte[] setPeid(byte[] peid) 
    {
        if (peid.length != PEID_LENGTH)
        {
            throw new NumberFormatException("Peid byte array length is "+peid.length+", must be "+PEID_LENGTH);
        }
        mPeid = peid;
        loaded = true;
        return mPeid;
    }
    /**
     * Return the current peid.  All 0 if never loaded, else last value set from 
     * a canonical or base64 representation 
     * 
     * @return byte[]
     */
    public byte[] getPeid() 
    {
        if (!loaded)
        {
            throw new NumberFormatException("No PEID has been loaded");
        }
        return mPeid;
    }

    /**
     * Return the current peid value as a 22 char unpadded Base64 string
     * 
     * @return String
     */
    public String getPeidBase64Unpadded() {
        if (!loaded)
        {
            throw new NumberFormatException("No PEID has been loaded");
        }
        
        Base64UrlEncoder b = new Base64UrlEncoder(mPeid);
        b.setEncodeUrl(false);
        b.setDropPadding(true);
        return b.processString();      
    }

    /**
     * Return the current peid value as a 24 char Base64 string
     * 
     * @return String
     */
    public String getPeidBase64() {
        if (!loaded)
        {
            throw new NumberFormatException("No PEID has been loaded");
        }
        
        Base64UrlEncoder b = new Base64UrlEncoder(mPeid);
        b.setEncodeUrl(false);
        b.setDropPadding(false);
        return b.processString();      
    }

    /**
     * Return the current peid value as a canonical string
     * 
     * @return String
     */
    public String getPeidCanonical()
    {
        /* Could be synchronized, but there isn't much harm in just taking
         * our chances (ie. in the worst case we'll form the string more
         * than once... but result is the same)
         */
        if (!loaded)
        {
            throw new NumberFormatException("No PEID has been loaded");
        }

        StringBuffer b = new StringBuffer(36);

        for (int i = 0; i < PEID_LENGTH; ++i)
        {
            // Need to bypass hyphens:
            switch (i) {
            case 4:
            case 6:
            case 8:
            case 10:
                b.append('-');
            }
            int toHex = mPeid[i] & 0xFF;
            b.append(kHexChars.charAt(toHex >> 4));
            b.append(kHexChars.charAt(toHex & 0x0f));
        }
        return b.toString();
    }

    /** 
    * Driver for conversions 
    * Command line args: output-format input 
    * where output-format is one of C (canonical) or B (base-64) 
    *   and input is either a 24 byte base64 formated PEID or a 36 byte canonical PEID 
    */
    public static void main(String[] args) {
        String usage = "Command line args: output-format input\r\n"
            + " where output-format is one of C (canonical), B (base-64, 24\r\n"
            + " characters), or U (base64 unpadded, 22 characters)\r\n"
            + "and input is a single or comma-delimited (no space) list of\r\n"
            + " a 24 byte base64 PEID, a 22 byte\r\n"
            + " a base64Unpadded formated PEID or a 36 byte canonical PEID"; 
        //Check args
        if (args.length != 2)
        {
            System.out.println("Wrong number of args");
            System.out.println(usage);
            System.exit(0);
        }
        
        //Try this
        //First, load peid with input value
        Peid peid = new Peid();
        String[] inputs = args[1].split(",");
        if (inputs.length == 0)
        {
            System.out.println("Found no elements in (comma-delimited) input");
            System.out.println(usage);
            System.exit(0);
        }
        //Process each input
        for (String input : inputs)
        {
            if (input.length() == 24)
            {
                try
                {
                    peid.setPeidBase64(input);
                } catch (java.io.IOException ioe)
                {
                    System.out.println("Conversion error, probably invalid base 64 string: "+ioe);
                    System.exit(1);
                } catch (org.w3c.tools.codec.Base64FormatException bfe)
                {
                    System.out.println("Conversion error, probably invalid base 64 string: "+bfe);
                    System.exit(1);
                }
            } else if (input.length() == 22)
            {
                try
                {
                    peid.setPeidBase64Unpadded(input);
                } catch (java.io.IOException ioe)
                {
                    System.out.println("Conversion error, probably invalid base 64 string: "+ioe);
                    System.exit(1);
                } catch (org.w3c.tools.codec.Base64FormatException bfe)
                {
                    System.out.println("Conversion error, probably invalid base 64 string: "+bfe);
                    System.exit(1);
                }
            } else if (input.length() == 36) {
                try
                {
                    peid.setPeidCanonical(input);
                } catch (NumberFormatException nfe)
                {
                    System.out.println("Conversion error, probably invalid canonical formatted string: "+nfe);
                    System.exit(1);              
                }
            } else {
                System.out.println("Invalid input format");
                System.out.println(usage);
                System.exit(0);
            }
            //Now display in requested output form
            if (args[0].equalsIgnoreCase("c"))
            {
                System.out.println(input+"="+peid.getPeidCanonical());
            } else if (args[0].equalsIgnoreCase("b"))
            {
                System.out.println(input+"="+peid.getPeidBase64());
            } else if (args[0].equalsIgnoreCase("u"))
            {
                System.out.println(input+"="+peid.getPeidBase64Unpadded());
    
            } else {
                System.out.println("Invalid output-format");
                System.out.println(usage);
                System.exit(0);
            }
        } //end for each input
    }
}

