package lol.moruto.scraper;

import java.util.HashMap;
import java.util.Map;

public enum CapeType {
    MINECON_2011("Minecon2011", "2011", 7, "http://textures.minecraft.net/texture/953cac8b779fe41383e675ee2b86071a71658f2180f56fbce8aa315ea70e2ed6"),
    MINECON_2012("Minecon2012", "2012", 6, "http://textures.minecraft.net/texture/a2e8d97ec79100e90a75d369d1b3ba81273c4f82bc1b737e934eed4a854be1b6"),
    MINECON_2013("Minecon2013", "2013", 5, "http://textures.minecraft.net/texture/153b1a0dfcbae953cdeb6f2c2bf6bf79943239b1372780da44bcbb29273131da"),
    MINECON_2015("Minecon2015", "2015", 4, "http://textures.minecraft.net/texture/b0cc08840700447322d953a02b965f1d65a13a603bf64b17c803c21446fe1635"),
    MINECON_2016("Minecon2016", "2016", 3, "http://textures.minecraft.net/texture/e7dfea16dc83c97df01a12fabbd1216359c0cd0ea42f9999b6e97c584963e980"),
    REALMS("MapMaker", "realms", 8, "http://textures.minecraft.net/texture/17912790ff164b93196f08ba71d0e62129304776d0f347334f8a6eae509f8a56"),
    MIGRATOR("Migrator", "migrator_cape", 1, "http://textures.minecraft.net/texture/2340c0e03dd24a11b15a8b33c2a7e9e32abb2051b2481d0ba7defd635ca7a933"),
    VANILLA("Vanilla", "vanilla", 1, "http://textures.minecraft.net/texture/f9a76537647989f9a0b6d001e320dac591c359e9e61a31f4ce11c88f207f0ad4"),
    CHERRY("Cherry Blossom", "cherry", 1, "http://textures.minecraft.net/texture/afd553b39358a24edfe3b8a9a939fa5fa4faa4d9a9c3d6af8eafb377fa05c2bb"),
    ANNIVERSARY("15th Anniversary", "15A", 1, "http://textures.minecraft.net/texture/cd9d82ab17fd92022dbd4a86cde4c382a7540e117fae7b9a2853658505a80625"),
    TWITCH("Purple Heart", "twitch", 1, "http://textures.minecraft.net/texture/cb40a92e32b57fd732a00fc325e7afb00a7ca74936ad50d8e860152e482cfbde"),
    TIKTOK("Follower’s", "tiktok", 1, "http://textures.minecraft.net/texture/569b7f2a1d00d26f30efe3f9ab9ac817b1e6d35f4f3cfb0324ef2d328223d350"),
    MCC("MCC 15th Year", "mcc", 1, "http://textures.minecraft.net/texture/56c35628fe1c4d59dd52561a3d03bfa4e1a76d397c8b9c476c2f77cb6aebb1df"),
    MCEXP("Minecraft Experience", "mcexp", 2, "http://textures.minecraft.net/texture/7658c5025c77cfac7574aab3af94a46a8886e3b7722a895255fbf22ab8652434"),
    MOJANG_OFFICE("Mojang Office", "mojangoffice", 1, "http://textures.minecraft.net/texture/5c29410057e32abec02d870ecb52ec25fb45ea81e785a7854ae8429d7236ca26"),
    MENACE("Menace", "menace", 1, "http://textures.minecraft.net/texture/dbc21e222528e30dc88445314f7be6ff12d3aeebc3c192054fba7e3b3f8c77b1"),
    HOME("Home", "home", 1, "http://textures.minecraft.net/texture/1de21419009db483900da6298a1e6cbf9f1bc1523a0dcdc16263fab150693edd"),
    YEARN("Yearn", "yearn", 1, "http://textures.minecraft.net/texture/308b32a9e303155a0b4262f9e5483ad4a22e3412e84fe8385a0bdd73dc41fa89"),
    COMMON("Common", "common", 1, "http://textures.minecraft.net/texture/5ec930cdd2629c8771655c60eebeb867b4b6559b0e6d3bc71c40c96347fa03f0"),
    FOUNDERS("Founder's", "founders", 1, "http://textures.minecraft.net/texture/99aba02ef05ec6aa4d42db8ee43796d6cd50e4b2954ab29f0caeb85f96bf52a1"),
    PAN("Pan", "pan", 1, "http://textures.minecraft.net/texture/28de4a81688ad18b49e735a273e086c18f1e3966956123ccb574034c06f5d336"),
    MOJANG_STUDIOS("Mojang-new", "mojangstudios", 9, "http://textures.minecraft.net/texture/9e507afc56359978a3eb3e32367042b853cddd0995d17d0da995662913fb00f7"),
    MOJANG("Mojang", "mojang", 9, "http://textures.minecraft.net/texture/5786fe99be377dfb6858859f926c4dbc995751e91cee373468c5fbf4865e7151"),
    MOJANG_OLD("Mojang-old", "mojangold", 11, "http://textures.minecraft.net/texture/8f120319222a9f4a104e2f5cb97b2cda93199a2ee9e1585cb8d09d6f687cb761"),
    SCROLLS("ScrollsChamp", "scrolls", 11, "http://textures.minecraft.net/texture/3efadf6510961830f9fcc077f19b4daf286d502b5f5aafbd807c7bbffcaca245"),
    MOJIRA("Moderator", "mojira", 10, "http://textures.minecraft.net/texture/ae677f7d98ac70a533713518416df4452fe5700365c09cf45d0d156ea9396551"),
    COBALT("Cobalt", "cobalt", 11, "http://textures.minecraft.net/texture/ca35c56efe71ed290385f4ab5346a1826b546a54d519e6a3ff01efa01acce81"),
    TRANSLATOR("Translator", "translator", 10, "http://textures.minecraft.net/texture/1bf91499701404e21bd46b0191d63239a4ef76ebde88d27e4d430ac211df681e"),
    TRANSLATOR_CHINESE("Translator-Chinese", "translatorchinese", 12, "http://textures.minecraft.net/texture/2262fb1d24912209490586ecae98aca8500df3eff91f2a07da37ee524e7e3cb6"),
    TRANSLATOR_JAPANESE("cheapsh0t", "translatorjapanese", 12, "http://textures.minecraft.net/texture/ca29f5dd9e94fb1748203b92e36b66fda80750c87ebc18d6eafdb0e28cc1d05f"),
    TURTLE("Turtle", "turtle", 12, "http://textures.minecraft.net/texture/5048ea61566353397247d2b7d946034de926b997d5e66c86483dfb1e031aee95"),
    PRISMARINE("Prismarine", "prismarine", 12, "http://textures.minecraft.net/texture/d8f8d13a1adf9636a16c31d47f3ecc9bb8d8533108aa5ad2a01b13b1a0c55eac"),
    SPADE("MrMessiah", "spade", 12, "http://textures.minecraft.net/texture/2e002d5e1758e79ba51d08d92a0f3a95119f2f435ae7704916507b6c565a7da8"),
    SNOWMAN("JulianClark", "snowman", 12, "http://textures.minecraft.net/texture/23ec737f18bfe4b547c95935fc297dd767bb84ee55bfd855144d279ac9bfd9fe"),
    DB("dannyBstyle", "dB", 12, "http://textures.minecraft.net/texture/bcfbe84c6542a4a5c213c1cacf8979b5e913dcb4ad783a8b80e3c4a7d5c8bdac"),
    MILLIONTH("MillionthSale", "millionth", 12, "http://textures.minecraft.net/texture/70efffaf86fe5bc089608d3cb297d3e276b9eb7a8f9f2fe6659c23a2d8b18edf"),
    BIRTHDAY("Birthday", "birthday", 12, "http://textures.minecraft.net/texture/2056f2eebd759cce93460907186ef44e9192954ae12b227d817eb4b55627a7fc"),
    VALENTINE("Valentine", "valentine", 12, "http://textures.minecraft.net/texture/e578ef995fabcf0a94768f9651ac3aaba30c59ef85b7f299b99c8e44c30d2ffb"),
    YOUTUBER("Youtuber", "youtuber", 12, "http://textures.minecraft.net/texture/6238f65ab798c34b6a7593565bcd945064d87edfb597ad4c4e1c8f53d4437b6b"),
    CAYNE("Cayne", "cayne", 13, "http://textures.minecraft.net/texture/a69b62596da8a49091e9297cc798086b9b9135da24a813a64da98cf1eac9d67b"),
    SWEDEN("Sweden", "sweden", 13, "http://textures.minecraft.net/texture/27b5c65e1bc388ca69799312b352072ea824b24c93ca4e8c428e23b23b77ff7c");

    private final String name;
    private final String code;

    CapeType(String name, String code, int level, String url) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    private static final Map<String, CapeType> CODE_LOOKUP = new HashMap<>();

    static {
        for (CapeType type : CapeType.values()) {
            CODE_LOOKUP.put(type.getCode().toLowerCase(), type);
        }
    }

    public static CapeType fromCode(String code) {
        return CODE_LOOKUP.get(code.toLowerCase());
    }

}
