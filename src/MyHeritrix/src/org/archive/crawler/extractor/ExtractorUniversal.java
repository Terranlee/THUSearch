/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Created on Jan 15, 2004
 *
 */
package org.archive.crawler.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;

import javax.management.AttributeNotFoundException;

import org.apache.commons.io.IOUtils;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.Type;
import org.archive.net.UURI;
import org.archive.util.TextUtils;

/**
 * A last ditch extractor that will look at the raw byte code and try to extract
 * anything that <i>looks</i> like a link.
 *
 * If used, it should always be specified as the last link extractor in the
 * order file.
 * <p>
 * To accomplish this it will scan through the bytecode and try and build up
 * strings of consecutive bytes that all represent characters that are valid
 * in a URL (see #isURLableChar(int) for details).
 * Once it hits the end of such a string (i.e. finds a character that
 * should not be in a URL) it will try to determine if it has found a URL.
 * This is done be seeing if the string is an IP address prefixed with
 * http(s):// or contains a dot followed by a Top Level Domain and end of
 * string or a slash.
 *
 * @author Kristinn Sigurdsson
 */
public class ExtractorUniversal extends Extractor
implements CoreAttributeConstants {

    private static final long serialVersionUID = -7593380118857156939L;

//    private static final Logger logger =
//        Logger.getLogger(ExtractorUniversal.class.getName());
    
    private static String ATTR_MAX_DEPTH_BYTES = "max-depth-bytes";

    /** Default value for how far into an unknown document we should scan
     * - 10k. A value of 0 or lower will disable this.
     */
    private static long DEFAULT_MAX_DEPTH_BYTES = 10240;

    private static String ATTR_MAX_URL_LENGTH = "max-url-length";

    /** Maximum length for a URI that we try to match.*/
    private static long DEFAULT_MAX_URL_LENGTH = UURI.MAX_URL_LENGTH;

    /**
     * Matches any string that begins with http:// or https:// followed by
     * something that looks like an ip address (four numbers, none longer then
     * 3 chars seperated by 3 dots). Does <b>not</b> ensure that the numbers are
     * each in the range 0-255.
     */
    static final String IP_ADDRESS =
        "((http://)|(https://))(\\d(\\d)?(\\d)?\\.\\d(\\d)?(\\d)?\\.\\d(\\d)?(\\d)?\\.\\d(\\d)?(\\d)?)";

    /**
     * Matches any string that begins with a TLD (no .) followed by a '/' slash
     * or end of string. If followed by slash then nothing after the slash is
     * of consequence.
     */
    public static final String TLDs =
          "(ac(/.*)?)"  // ac  Ascension Island
        + "|(ad(/.*)?)" // ad  Andorra
        + "|(ae(/.*)?)" // ae  United Arab Emirates
        + "|(af(/.*)?)" // af  Afghanistan
        + "|(ag(/.*)?)" // ag  Antigua and Barbuda
        + "|(ai(/.*)?)" // ai  Anguilla
        + "|(al(/.*)?)" // al  Albania
        + "|(am(/.*)?)" // am  Armenia
        + "|(an(/.*)?)" // an  Netherlands Antilles
        + "|(ao(/.*)?)" // ao  Angola
        + "|(aero(/.*)?)" // aero Air-transport industry
        + "|(aq(/.*)?)" // aq  Antarctica
        + "|(ar(/.*)?)" // ar  Argentina
        + "|(as(/.*)?)" // as  American Samoa
        + "|(at(/.*)?)" // at  Austria
        + "|(au(/.*)?)" // au  Australia
        + "|(aw(/.*)?)" // aw  Aruba
        + "|(az(/.*)?)" // az  Azerbaijan
        + "|(ba(/.*)?)" // ba  Bosnia Hercegovina
        + "|(bb(/.*)?)" // bb  Barbados
        + "|(bd(/.*)?)" // bd  Bangladesh
        + "|(be(/.*)?)" // be  Belgium
        + "|(bf(/.*)?)" // bf  Burkina Faso
        + "|(bg(/.*)?)" // bg  Bulgaria
        + "|(bh(/.*)?)" // bh  Bahrain
        + "|(bi(/.*)?)" // bi  Burundi
        + "|(biz(/.*)?)" // biz Businesses
        + "|(bj(/.*)?)" // bj  Benin
        + "|(bm(/.*)?)" // bm  Bermuda
        + "|(bn(/.*)?)" // bn  Brunei Darussalam
        + "|(bo(/.*)?)" // bo  Bolivia
        + "|(br(/.*)?)" // br  Brazil
        + "|(bs(/.*)?)" // bs  Bahamas
        + "|(bt(/.*)?)" // bt  Bhutan
        + "|(bv(/.*)?)" // bv  Bouvet Island
        + "|(bw(/.*)?)" // bw  Botswana
        + "|(by(/.*)?)" // by  Belarus (Byelorussia)
        + "|(bz(/.*)?)" // bz  Belize
        + "|(ca(/.*)?)" // ca  Canada
        + "|(cc(/.*)?)" // cc  Cocos Islands (Keeling)
        + "|(cd(/.*)?)" // cd  Congo, Democratic Republic of the
        + "|(cf(/.*)?)" // cf  Central African Republic
        + "|(cg(/.*)?)" // cg  Congo, Republic of
        + "|(ch(/.*)?)" // ch  Switzerland
        + "|(ci(/.*)?)" // ci  Cote d'Ivoire (Ivory Coast)
        + "|(ck(/.*)?)" // ck  Cook Islands
        + "|(cl(/.*)?)" // cl  Chile
        + "|(cm(/.*)?)" // cm  Cameroon
        + "|(cn(/.*)?)" // cn  China
        + "|(co(/.*)?)" // co  Colombia
        + "|(com(/.*)?)" // com Commercial
        + "|(coop(/.*)?)" // coop Cooperatives
        + "|(cr(/.*)?)" // cr  Costa Rica
        + "|(cs(/.*)?)" // cs  Czechoslovakia
        + "|(cu(/.*)?)" // cu  Cuba
        + "|(cv(/.*)?)" // cv  Cap Verde
        + "|(cx(/.*)?)" // cx  Christmas Island
        + "|(cy(/.*)?)" // cy  Cyprus
        + "|(cz(/.*)?)" // cz  Czech Republic
        + "|(de(/.*)?)" // de  Germany
        + "|(dj(/.*)?)" // dj  Djibouti
        + "|(dk(/.*)?)" // dk  Denmark
        + "|(dm(/.*)?)" // dm  Dominica
        + "|(do(/.*)?)" // do  Dominican Republic
        + "|(dz(/.*)?)" // dz  Algeria
        + "|(ec(/.*)?)" // ec  Ecuador
        + "|(edu(/.*)?)" // edu Educational Institution
        + "|(ee(/.*)?)" // ee  Estonia
        + "|(eg(/.*)?)" // eg  Egypt
        + "|(eh(/.*)?)" // eh  Western Sahara
        + "|(er(/.*)?)" // er  Eritrea
        + "|(es(/.*)?)" // es  Spain
        + "|(et(/.*)?)" // et  Ethiopia
        + "|(fi(/.*)?)" // fi  Finland
        + "|(fj(/.*)?)" // fj  Fiji
        + "|(fk(/.*)?)" // fk  Falkland Islands
        + "|(fm(/.*)?)" // fm  Micronesia, Federal State of
        + "|(fo(/.*)?)" // fo  Faroe Islands
        + "|(fr(/.*)?)" // fr  France
        + "|(ga(/.*)?)" // ga  Gabon
        + "|(gd(/.*)?)" // gd  Grenada
        + "|(ge(/.*)?)" // ge  Georgia
        + "|(gf(/.*)?)" // gf  French Guiana
        + "|(gg(/.*)?)" // gg  Guernsey
        + "|(gh(/.*)?)" // gh  Ghana
        + "|(gi(/.*)?)" // gi  Gibraltar
        + "|(gl(/.*)?)" // gl  Greenland
        + "|(gm(/.*)?)" // gm  Gambia
        + "|(gn(/.*)?)" // gn  Guinea
        + "|(gov(/.*)?)" // gov Government (US)
        + "|(gp(/.*)?)" // gp  Guadeloupe
        + "|(gq(/.*)?)" // gq  Equatorial Guinea
        + "|(gr(/.*)?)" // gr  Greece
        + "|(gs(/.*)?)" // gs  South Georgia and the South Sandwich Islands
        + "|(gt(/.*)?)" // gt  Guatemala
        + "|(gu(/.*)?)" // gu  Guam
        + "|(gw(/.*)?)" // gw  Guinea-Bissau
        + "|(gy(/.*)?)" // gy  Guyana
        + "|(hk(/.*)?)" // hk  Hong Kong
        + "|(hm(/.*)?)" // hm  Heard and McDonald Islands
        + "|(hn(/.*)?)" // hn  Honduras
        + "|(hr(/.*)?)" // hr  Croatia/Hrvatska
        + "|(ht(/.*)?)" // ht  Haiti
        + "|(hu(/.*)?)" // hu  Hungary
        + "|(id(/.*)?)" // id  Indonesia
        + "|(ie(/.*)?)" // ie  Ireland
        + "|(il(/.*)?)" // il  Israel
        + "|(im(/.*)?)" // im  Isle of Man
        + "|(in(/.*)?)" // in  India
        + "|(info(/.*)?)" // info
        + "|(int(/.*)?)" // int Int. Organizations
        + "|(io(/.*)?)" // io  British Indian Ocean Territory
        + "|(iq(/.*)?)" // iq  Iraq
        + "|(ir(/.*)?)" // ir  Iran, Islamic Republic of
        + "|(is(/.*)?)" // is  Iceland
        + "|(it(/.*)?)" // it  Italy
        + "|(je(/.*)?)" // je  Jersey
        + "|(jm(/.*)?)" // jm  Jamaica
        + "|(jo(/.*)?)" // jo  Jordan
        + "|(jp(/.*)?)" // jp  Japan
        + "|(ke(/.*)?)" // ke  Kenya
        + "|(kg(/.*)?)" // kg  Kyrgyzstan
        + "|(kh(/.*)?)" // kh  Cambodia
        + "|(ki(/.*)?)" // ki  Kiribati
        + "|(km(/.*)?)" // km  Comoros
        + "|(kn(/.*)?)" // kn  Saint Kitts and Nevis
        + "|(kp(/.*)?)" // kp  Korea, Democratic People's Republic
        + "|(kr(/.*)?)" // kr  Korea, Republic of
        + "|(kw(/.*)?)" // kw  Kuwait
        + "|(ky(/.*)?)" // ky  Cayman Islands
        + "|(kz(/.*)?)" // kz  Kazakhstan
        + "|(la(/.*)?)" // la  Lao People's Democratic Republic
        + "|(lb(/.*)?)" // lb  Lebanon
        + "|(lc(/.*)?)" // lc  Saint Lucia
        + "|(li(/.*)?)" // li  Liechtenstein
        + "|(lk(/.*)?)" // lk  Sri Lanka
        + "|(lr(/.*)?)" // lr  Liberia
        + "|(ls(/.*)?)" // ls  Lesotho
        + "|(lt(/.*)?)" // lt  Lithuania
        + "|(lu(/.*)?)" // lu  Luxembourg
        + "|(lv(/.*)?)" // lv  Latvia
        + "|(ly(/.*)?)" // ly  Libyan Arab Jamahiriya
        + "|(ma(/.*)?)" // ma  Morocco
        + "|(mc(/.*)?)" // mc  Monaco
        + "|(md(/.*)?)" // md  Moldova, Republic of
        + "|(mg(/.*)?)" // mg  Madagascar
        + "|(mh(/.*)?)" // mh  Marshall Islands
        + "|(mil(/.*)?)" // mil Military (US Dept of Defense)
        + "|(mk(/.*)?)" // mk  Macedonia, Former Yugoslav Republic
        + "|(ml(/.*)?)" // ml  Mali
        + "|(mm(/.*)?)" // mm  Myanmar
        + "|(mn(/.*)?)" // mn  Mongolia
        + "|(mo(/.*)?)" // mo  Macau
        + "|(mp(/.*)?)" // mp  Northern Mariana Islands
        + "|(mq(/.*)?)" // mq  Martinique
        + "|(mr(/.*)?)" // mr  Mauritani
        + "|(ms(/.*)?)" // ms  Montserrat
        + "|(mt(/.*)?)" // mt  Malta
        + "|(mu(/.*)?)" // mu  Mauritius
        + "|(museum(/.*)?)" // museum Museums
        + "|(mv(/.*)?)" // mv  Maldives
        + "|(mw(/.*)?)" // mw  Malawi
        + "|(mx(/.*)?)" // mx  Mexico
        + "|(my(/.*)?)" // my  Malaysia
        + "|(mz(/.*)?)" // mz  Mozambique
        + "|(na(/.*)?)" // na  Namibia
        + "|(name(/.*)?)" // name Individuals
        + "|(nc(/.*)?)" // nc  New Caledonia
        + "|(ne(/.*)?)" // ne  Niger
        + "|(net(/.*)?)" // net networks
        + "|(nf(/.*)?)" // nf  Norfolk Island
        + "|(ng(/.*)?)" // ng  Nigeria
        + "|(ni(/.*)?)" // ni  Nicaragua
        + "|(nl(/.*)?)" // nl  Netherlands
        + "|(no(/.*)?)" // no  Norway
        + "|(np(/.*)?)" // np  Nepal
        + "|(nr(/.*)?)" // nr  Nauru
        + "|(nt(/.*)?)" // nt  Neutral Zone
        + "|(nu(/.*)?)" // nu  Niue
        + "|(nz(/.*)?)" // nz  New Zealand
        + "|(om(/.*)?)" // om  Oman
        + "|(org(/.*)?)" // org Organization (non-profit)
        + "|(pa(/.*)?)" // pa  Panama
        + "|(pe(/.*)?)" // pe  Peru
        + "|(pf(/.*)?)" // pf  French Polynesia
        + "|(pg(/.*)?)" // pg  Papua New Guinea
        + "|(ph(/.*)?)" // ph  Philippines
        + "|(pk(/.*)?)" // pk  Pakistan
        + "|(pl(/.*)?)" // pl  Poland
        + "|(pm(/.*)?)" // pm  St. Pierre and Miquelon
        + "|(pn(/.*)?)" // pn  Pitcairn Island
        + "|(pr(/.*)?)" // pr  Puerto Rico
        + "|(pro(/.*)?)" // pro Accountants, lawyers, and physicians
        + "|(ps(/.*)?)" // ps  Palestinian Territories
        + "|(pt(/.*)?)" // pt  Portugal
        + "|(pw(/.*)?)" // pw  Palau
        + "|(py(/.*)?)" // py  Paraguay
        + "|(qa(/.*)?)" // qa  Qatar
        + "|(re(/.*)?)" // re  Reunion Island
        + "|(ro(/.*)?)" // ro  Romania
        + "|(ru(/.*)?)" // ru  Russian Federation
        + "|(rw(/.*)?)" // rw  Rwanda
        + "|(sa(/.*)?)" // sa  Saudi Arabia
        + "|(sb(/.*)?)" // sb  Solomon Islands
        + "|(sc(/.*)?)" // sc  Seychelles
        + "|(sd(/.*)?)" // sd  Sudan
        + "|(se(/.*)?)" // se  Sweden
        + "|(sg(/.*)?)" // sg  Singapore
        + "|(sh(/.*)?)" // sh  St. Helena
        + "|(si(/.*)?)" // si  Slovenia
        + "|(sj(/.*)?)" // sj  Svalbard and Jan Mayen Islands
        + "|(sk(/.*)?)" // sk  Slovak Republic
        + "|(sl(/.*)?)" // sl  Sierra Leone
        + "|(sm(/.*)?)" // sm  San Marino
        + "|(sn(/.*)?)" // sn  Senegal
        + "|(so(/.*)?)" // so  Somalia
        + "|(sr(/.*)?)" // sr  Suriname
        + "|(sv(/.*)?)" // sv  El Salvador
        + "|(st(/.*)?)" // st  Sao Tome and Principe
        + "|(sy(/.*)?)" // sy  Syrian Arab Republic
        + "|(sz(/.*)?)" // sz  Swaziland
        + "|(tc(/.*)?)" // tc  Turks and Caicos Islands
        + "|(td(/.*)?)" // td  Chad
        + "|(tf(/.*)?)" // tf  French Southern Territories
        + "|(tg(/.*)?)" // tg  Togo
        + "|(th(/.*)?)" // th  Thailand
        + "|(tj(/.*)?)" // tj  Tajikistan
        + "|(tk(/.*)?)" // tk  Tokelau
        + "|(tm(/.*)?)" // tm  Turkmenistan
        + "|(tn(/.*)?)" // tn  Tunisia
        + "|(to(/.*)?)" // to  Tonga
        + "|(tp(/.*)?)" // tp  East Timor
        + "|(tr(/.*)?)" // tr  Turkey
        + "|(tt(/.*)?)" // tt  Trinidad and Tobago
        + "|(tv(/.*)?)" // tv  Tuvalu
        + "|(tw(/.*)?)" // tw  Taiwan
        + "|(tz(/.*)?)" // tz  Tanzania
        + "|(ua(/.*)?)" // ua  Ukraine
        + "|(ug(/.*)?)" // ug  Uganda
        + "|(uk(/.*)?)" // uk  United Kingdom
        + "|(um(/.*)?)" // um  US Minor Outlying Islands
        + "|(us(/.*)?)" // us  United States
        + "|(uy(/.*)?)" // uy  Uruguay
        + "|(uz(/.*)?)" // uz  Uzbekistan
        + "|(va(/.*)?)" // va  Holy See (City Vatican State)
        + "|(vc(/.*)?)" // vc  Saint Vincent and the Grenadines
        + "|(ve(/.*)?)" // ve  Venezuela
        + "|(vg(/.*)?)" // vg  Virgin Islands (British)
        + "|(vi(/.*)?)" // vi  Virgin Islands (USA)
        + "|(vn(/.*)?)" // vn  Vietnam
        + "|(vu(/.*)?)" // vu  Vanuatu
        + "|(wf(/.*)?)" // wf  Wallis and Futuna Islands
        + "|(ws(/.*)?)" // ws  Western Samoa
        + "|(ye(/.*)?)" // ye  Yemen
        + "|(yt(/.*)?)" // yt  Mayotte
        + "|(yu(/.*)?)" // yu  Yugoslavia
        + "|(za(/.*)?)" // za  South Africa
        + "|(zm(/.*)?)" // zm  Zambia
        + "|(zw(/.*)?)" // zw  Zimbabwe
        ;

    protected long numberOfCURIsHandled = 0;
    protected long numberOfLinksExtracted= 0;

    /**
     * Constructor
     * @param name The name of the module.
     */
    public ExtractorUniversal(String name) {
        super(name, "Link extraction on unknown file types. A best effort" +
                " extractor that looks at the raw byte code of any file " +
                "that has not been handled by another extractor and tries" +
                " to find URIs. Will only match absolute URIs.");
        Type e;
        e = addElementToDefinition(new SimpleType(ATTR_MAX_DEPTH_BYTES,
            "How deep to look into files for URI strings, in bytes",
            new Long(DEFAULT_MAX_DEPTH_BYTES)));
        e.setExpertSetting(true);
        e = addElementToDefinition(new SimpleType(ATTR_MAX_URL_LENGTH,
            "Max length of URIs in bytes", new Long(DEFAULT_MAX_URL_LENGTH)));
        e.setExpertSetting(true);
    }

    protected void extract(CrawlURI curi) {
        if (!isHttpTransactionContentToProcess(curi)) {
            return;
        }

        numberOfCURIsHandled++;

        InputStream instream = null;
        try {
            instream = curi.getHttpRecorder().getRecordedInput().
                getContentReplayInputStream();
            int ch = instream.read();
            StringBuffer lookat = new StringBuffer();
            long counter = 0;
            long maxdepth = ((Long)getAttribute(ATTR_MAX_DEPTH_BYTES,curi)).
                longValue();
            if(maxdepth<=0){
                maxdepth = Long.MAX_VALUE;
            }
            long maxURLLength = ((Long)getAttribute(ATTR_MAX_URL_LENGTH,curi)).
                longValue();
            boolean foundDot = false;
            while(ch != -1 && ++counter <= maxdepth) {
                if(lookat.length()>maxURLLength){
                    //Exceeded maximum length of a URL. Start fresh.
                    lookat = new StringBuffer();
                    foundDot = false;
                }
                else if(isURLableChar(ch)){
                    //Add to buffer.
                    if(ch == 46){
                        // Current character is a dot '.'
                        foundDot = true;
                    }
                    lookat.append((char)ch);
                } else if(lookat.length() > 3 && foundDot) {
                    // It takes a bare mininum of 4 characters to form a URL
                    // Since we have at least that many let's try link
                    // extraction.
                    String newURL = lookat.toString();
                    if(looksLikeAnURL(newURL))
                    {
                        // Looks like we found something.

                        // Let's start with a little cleanup as we may have
                        // junk in front or at the end.
                        if(newURL.toLowerCase().indexOf("http") > 0){
                            // Got garbage in front of the protocol. Remove.
                            newURL = newURL.substring(newURL.toLowerCase().
                                indexOf("http"));
                        }
                        while(newURL.substring(newURL.length()-1).equals("."))
                        {
                            // URLs can't end with a dot. Strip it off.
                            newURL = newURL.substring(0,newURL.length()-1);
                        }

                        // And add the URL to speculative embeds.
                        numberOfLinksExtracted++;
                        curi.createAndAddLink(newURL,Link.SPECULATIVE_MISC,Link.SPECULATIVE_HOP);
                    }
                    // Reset lookat for next string.
                    lookat = new StringBuffer();
                    foundDot = false;
                } else if(lookat.length()>0) {
                    // Didn't get enough chars. Reset lookat for next string.
                    lookat = new StringBuffer();
                    foundDot = false;
                }
                ch = instream.read();
            }
        } catch(IOException e){
            //TODO: Handle this exception.
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(instream);
        }
        // Set flag to indicate that link extraction is completed.
        curi.linkExtractorFinished();
    }

    /**
     * This method takes a look at a string and determines if it could be a URL.
     * To qualify the string must either begin with "http://" (https would also
     * work) followed by something that looks like an IP address or contain
     * within the string (possible at the end but not at the beginning) a TLD
     * (Top Level Domain) preceded by a dot.
     *
     * @param lookat The string to examine in an effort to determine if it
     * could be a URL
     * @return True if the string matches the above criteria for a URL.
     */
    private boolean looksLikeAnURL(String lookat) {
        if(lookat.indexOf("http://")==0 || lookat.indexOf("https://")==0){
            //Check if the rest of the string looks like an IP address.
            //if so return true. Otherwise continue on.
            Matcher ip = TextUtils.getMatcher(IP_ADDRESS, lookat);
            boolean testVal = ip.matches();
            TextUtils.recycleMatcher(ip);
            if(testVal){
                return true;
            }
        }

        int dot = lookat.indexOf(".");
        if(dot!=0){//An URL can't start with a .tld.
            while(dot != -1 && dot < lookat.length()){
                lookat = lookat.substring(dot+1);
                if (isTLD(lookat.substring(0, lookat.length() <= 6?
                    lookat.length(): 6)))
                {
                    return true;
                }
                dot = lookat.indexOf(".");
            }
        }

        return false;
    }

    /**
     * Checks if a string is equal to known Top Level Domain. The string may
     * contain additional characters <i>after</i> the TLD but not before.
     * @param potentialTLD The string (usually 2-6 chars) to check if it starts
     * with a TLD.
     * @return True if the given string starts with the name of a known TLD
     *
     * @see #TLDs
     */
    private boolean isTLD(String potentialTLD) {
        if(potentialTLD.length()<2){
            return false;
        }

        potentialTLD.toLowerCase();
        Matcher uri = TextUtils.getMatcher(TLDs, potentialTLD);
        boolean ret = uri.matches();
        TextUtils.recycleMatcher(uri);
        return ret;
    }

    /**
     * Determines if a char (as represented by an int in the range of 0-255) is
     * a character (in the Ansi character set) that can be present in a URL.
     * This method takes a <b>strict</b> approach to what characters can be in
     * a URL.
     * <p>
     * The following are considered to be 'URLable'<br>
     * <ul>
     *  <li> <code># $ % & + , - . /</code> values 35-38,43-47
     *  <li> <code>[0-9]</code> values 48-57
     *  <li> <code>: ; = ? @</code> value 58-59,61,63-64
     *  <li> <code>[A-Z]</code> values 65-90
     *  <li> <code>_</code> value 95
     *  <li> <code>[a-z]</code> values 97-122
     *  <li> <code>~</code> value 126
     * </ul>
     * <p>
     * To summerize, the following ranges are considered URLable:<br>
     * 35-38,43-59,61,63-90,95,97-122,126
     *
     * @param ch The character (represented by an int) to test.
     * @return True if it is a URLable character, false otherwise.
     */
    private boolean isURLableChar(int ch) {
        return (ch>=35 && ch<=38)
            || (ch>=43 && ch<=59)
            || (ch==61)
            || (ch>=63 && ch<=90)
            || (ch==95)
            || (ch>=97 && ch<=122)
            || (ch==126);
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: org.archive.crawler.extractor." +
            "ExtractorUniversal\n");
        ret.append("  Function:          Link extraction on unknown file" +
            " types.\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
