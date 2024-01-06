package work.socialhub.planetlink.define

/**
 * Special char for xml
 * XHTML 内の特殊文字定義
 *
 * @link http://www.w3.org/TR/html4/charset.html
 */
enum class SpecialCharType(
    val entityRepl: String,
    val numberRepl: String,
) {
    // Auto Generated Code
    // see ReferGenerator.java (Test)
    nbsp("&nbsp;", "&#160;"),
    iexcl("&iexcl;", "&#161;"),
    cent("&cent;", "&#162;"),
    pound("&pound;", "&#163;"),
    curren("&curren;", "&#164;"),
    yen("&yen;", "&#165;"),
    brvbar("&brvbar;", "&#166;"),
    sect("&sect;", "&#167;"),
    uml("&uml;", "&#168;"),
    copy("&copy;", "&#169;"),
    ordf("&ordf;", "&#170;"),
    laquo("&laquo;", "&#171;"),
    not("&not;", "&#172;"),
    shy("&shy;", "&#173;"),
    reg("&reg;", "&#174;"),
    macr("&macr;", "&#175;"),
    deg("&deg;", "&#176;"),
    plusmn("&plusmn;", "&#177;"),
    sup2("&sup2;", "&#178;"),
    sup3("&sup3;", "&#179;"),
    acute("&acute;", "&#180;"),
    micro("&micro;", "&#181;"),
    para("&para;", "&#182;"),
    middot("&middot;", "&#183;"),
    cedil("&cedil;", "&#184;"),
    sup1("&sup1;", "&#185;"),
    ordm("&ordm;", "&#186;"),
    raquo("&raquo;", "&#187;"),
    frac14("&frac14;", "&#188;"),
    frac12("&frac12;", "&#189;"),
    frac34("&frac34;", "&#190;"),
    iquest("&iquest;", "&#191;"),
    Agrave("&Agrave;", "&#192;"),
    Aacute("&Aacute;", "&#193;"),
    Acirc("&Acirc;", "&#194;"),
    Atilde("&Atilde;", "&#195;"),
    Auml("&Auml;", "&#196;"),
    Aring("&Aring;", "&#197;"),
    AElig("&AElig;", "&#198;"),
    Ccedil("&Ccedil;", "&#199;"),
    Egrave("&Egrave;", "&#200;"),
    Eacute("&Eacute;", "&#201;"),
    Ecirc("&Ecirc;", "&#202;"),
    Euml("&Euml;", "&#203;"),
    Igrave("&Igrave;", "&#204;"),
    Iacute("&Iacute;", "&#205;"),
    Icirc("&Icirc;", "&#206;"),
    Iuml("&Iuml;", "&#207;"),
    ETH("&ETH;", "&#208;"),
    Ntilde("&Ntilde;", "&#209;"),
    Ograve("&Ograve;", "&#210;"),
    Oacute("&Oacute;", "&#211;"),
    Ocirc("&Ocirc;", "&#212;"),
    Otilde("&Otilde;", "&#213;"),
    Ouml("&Ouml;", "&#214;"),
    times("&times;", "&#215;"),
    Oslash("&Oslash;", "&#216;"),
    Ugrave("&Ugrave;", "&#217;"),
    Uacute("&Uacute;", "&#218;"),
    Ucirc("&Ucirc;", "&#219;"),
    Uuml("&Uuml;", "&#220;"),
    Yacute("&Yacute;", "&#221;"),
    THORN("&THORN;", "&#222;"),
    szlig("&szlig;", "&#223;"),
    agrave("&agrave;", "&#224;"),
    aacute("&aacute;", "&#225;"),
    acirc("&acirc;", "&#226;"),
    atilde("&atilde;", "&#227;"),
    auml("&auml;", "&#228;"),
    aring("&aring;", "&#229;"),
    aelig("&aelig;", "&#230;"),
    ccedil("&ccedil;", "&#231;"),
    egrave("&egrave;", "&#232;"),
    eacute("&eacute;", "&#233;"),
    ecirc("&ecirc;", "&#234;"),
    euml("&euml;", "&#235;"),
    igrave("&igrave;", "&#236;"),
    iacute("&iacute;", "&#237;"),
    icirc("&icirc;", "&#238;"),
    iuml("&iuml;", "&#239;"),
    eth("&eth;", "&#240;"),
    ntilde("&ntilde;", "&#241;"),
    ograve("&ograve;", "&#242;"),
    oacute("&oacute;", "&#243;"),
    ocirc("&ocirc;", "&#244;"),
    otilde("&otilde;", "&#245;"),
    ouml("&ouml;", "&#246;"),
    divide("&divide;", "&#247;"),
    oslash("&oslash;", "&#248;"),
    ugrave("&ugrave;", "&#249;"),
    uacute("&uacute;", "&#250;"),
    ucirc("&ucirc;", "&#251;"),
    uuml("&uuml;", "&#252;"),
    yacute("&yacute;", "&#253;"),
    thorn("&thorn;", "&#254;"),
    yuml("&yuml;", "&#255;"),
    fnof("&fnof;", "&#402;"),
    Alpha("&Alpha;", "&#913;"),
    Beta("&Beta;", "&#914;"),
    Gamma("&Gamma;", "&#915;"),
    Delta("&Delta;", "&#916;"),
    Epsilon("&Epsilon;", "&#917;"),
    Zeta("&Zeta;", "&#918;"),
    Eta("&Eta;", "&#919;"),
    Theta("&Theta;", "&#920;"),
    Iota("&Iota;", "&#921;"),
    Kappa("&Kappa;", "&#922;"),
    Lambda("&Lambda;", "&#923;"),
    Mu("&Mu;", "&#924;"),
    Nu("&Nu;", "&#925;"),
    Xi("&Xi;", "&#926;"),
    Omicron("&Omicron;", "&#927;"),
    Pi("&Pi;", "&#928;"),
    Rho("&Rho;", "&#929;"),
    Sigma("&Sigma;", "&#931;"),
    Tau("&Tau;", "&#932;"),
    Upsilon("&Upsilon;", "&#933;"),
    Phi("&Phi;", "&#934;"),
    Chi("&Chi;", "&#935;"),
    Psi("&Psi;", "&#936;"),
    Omega("&Omega;", "&#937;"),
    alpha("&alpha;", "&#945;"),
    beta("&beta;", "&#946;"),
    gamma("&gamma;", "&#947;"),
    delta("&delta;", "&#948;"),
    epsilon("&epsilon;", "&#949;"),
    zeta("&zeta;", "&#950;"),
    eta("&eta;", "&#951;"),
    theta("&theta;", "&#952;"),
    iota("&iota;", "&#953;"),
    kappa("&kappa;", "&#954;"),
    lambda("&lambda;", "&#955;"),
    mu("&mu;", "&#956;"),
    nu("&nu;", "&#957;"),
    xi("&xi;", "&#958;"),
    omicron("&omicron;", "&#959;"),
    pi("&pi;", "&#960;"),
    rho("&rho;", "&#961;"),
    sigmaf("&sigmaf;", "&#962;"),
    sigma("&sigma;", "&#963;"),
    tau("&tau;", "&#964;"),
    upsilon("&upsilon;", "&#965;"),
    phi("&phi;", "&#966;"),
    chi("&chi;", "&#967;"),
    psi("&psi;", "&#968;"),
    omega("&omega;", "&#969;"),
    thetasym("&thetasym;", "&#977;"),
    upsih("&upsih;", "&#978;"),
    piv("&piv;", "&#982;"),
    bull("&bull;", "&#8226;"),
    hellip("&hellip;", "&#8230;"),
    prime("&prime;", "&#8242;"),
    Prime("&Prime;", "&#8243;"),
    oline("&oline;", "&#8254;"),
    frasl("&frasl;", "&#8260;"),
    weierp("&weierp;", "&#8472;"),
    image("&image;", "&#8465;"),
    real("&real;", "&#8476;"),
    trade("&trade;", "&#8482;"),
    alefsym("&alefsym;", "&#8501;"),
    larr("&larr;", "&#8592;"),
    uarr("&uarr;", "&#8593;"),
    rarr("&rarr;", "&#8594;"),
    darr("&darr;", "&#8595;"),
    harr("&harr;", "&#8596;"),
    crarr("&crarr;", "&#8629;"),
    lArr("&lArr;", "&#8656;"),
    uArr("&uArr;", "&#8657;"),
    rArr("&rArr;", "&#8658;"),
    dArr("&dArr;", "&#8659;"),
    hArr("&hArr;", "&#8660;"),
    forall("&forall;", "&#8704;"),
    part("&part;", "&#8706;"),
    exist("&exist;", "&#8707;"),
    empty("&empty;", "&#8709;"),
    nabla("&nabla;", "&#8711;"),
    isin("&isin;", "&#8712;"),
    notin("&notin;", "&#8713;"),
    ni("&ni;", "&#8715;"),
    prod("&prod;", "&#8719;"),
    sum("&sum;", "&#8721;"),
    minus("&minus;", "&#8722;"),
    lowast("&lowast;", "&#8727;"),
    radic("&radic;", "&#8730;"),
    prop("&prop;", "&#8733;"),
    infin("&infin;", "&#8734;"),
    ang("&ang;", "&#8736;"),
    and("&and;", "&#8743;"),
    or("&or;", "&#8744;"),
    cap("&cap;", "&#8745;"),
    cup("&cup;", "&#8746;"),

    // int("&int;", "&#8747;"),
    SCint("&int;", "&#8747;"),

    there4("&there4;", "&#8756;"),
    sim("&sim;", "&#8764;"),
    cong("&cong;", "&#8773;"),
    asymp("&asymp;", "&#8776;"),
    ne("&ne;", "&#8800;"),
    equiv("&equiv;", "&#8801;"),
    le("&le;", "&#8804;"),
    ge("&ge;", "&#8805;"),
    sub("&sub;", "&#8834;"),
    sup("&sup;", "&#8835;"),
    nsub("&nsub;", "&#8836;"),
    sube("&sube;", "&#8838;"),
    supe("&supe;", "&#8839;"),
    oplus("&oplus;", "&#8853;"),
    otimes("&otimes;", "&#8855;"),
    perp("&perp;", "&#8869;"),
    sdot("&sdot;", "&#8901;"),
    lceil("&lceil;", "&#8968;"),
    rceil("&rceil;", "&#8969;"),
    lfloor("&lfloor;", "&#8970;"),
    rfloor("&rfloor;", "&#8971;"),
    lang("&lang;", "&#9001;"),
    rang("&rang;", "&#9002;"),
    loz("&loz;", "&#9674;"),
    spades("&spades;", "&#9824;"),
    clubs("&clubs;", "&#9827;"),
    hearts("&hearts;", "&#9829;"),
    diams("&diams;", "&#9830;"),
    quot("&quot;", "&#34;"),
    amp("&amp;", "&#38;"),

    // #39("&#39;", "&#39;"),
    SC39("&#39;", "&#39;"),

    lt("&lt;", "&#60;"),
    gt("&gt;", "&#62;"),
    OElig("&OElig;", "&#338;"),
    oelig("&oelig;", "&#339;"),
    Scaron("&Scaron;", "&#352;"),
    scaron("&scaron;", "&#353;"),
    Yuml("&Yuml;", "&#376;"),
    circ("&circ;", "&#710;"),
    tilde("&tilde;", "&#732;"),
    ensp("&ensp;", "&#8194;"),
    emsp("&emsp;", "&#8195;"),
    thinsp("&thinsp;", "&#8201;"),
    zwnj("&zwnj;", "&#8204;"),
    zwj("&zwj;", "&#8205;"),
    lrm("&lrm;", "&#8206;"),
    rlm("&rlm;", "&#8207;"),
    ndash("&ndash;", "&#8211;"),
    mdash("&mdash;", "&#8212;"),
    lsquo("&lsquo;", "&#8216;"),
    rsquo("&rsquo;", "&#8217;"),
    sbquo("&sbquo;", "&#8218;"),
    ldquo("&ldquo;", "&#8220;"),
    rdquo("&rdquo;", "&#8221;"),
    bdquo("&bdquo;", "&#8222;"),
    dagger("&dagger;", "&#8224;"),
    Dagger("&Dagger;", "&#8225;"),
    permil("&permil;", "&#8240;"),
    lsaquo("&lsaquo;", "&#8249;"),
    rsaquo("&rsaquo;", "&#8250;"),
    euro("&euro;", "&#8364;"),
}
