package main;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.atilika.kuromoji.TokenizerBase.Mode;
import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.atilika.kuromoji.ipadic.Tokenizer.Builder;

public class Test {

	// kuromojiのインスタンス
	private static Tokenizer tokenizer;

	// 記号正規表現
	public static final String SYMBOL_REGEX = "^[ -/:-@\\[-\\`\\{-\\~’‘“”＃＄％＆－―‐＾；：，、．。？！＠／￥＝￣｜＿＋＊（）［］｛｝＜＞「」『』【】〔〕〈〉《》]+$";

	public static void main(String[] args) {

		// kuromojiのインスタンス生成
		Builder builder = new Builder();
		builder.mode(Mode.NORMAL);
//      // Searchモード
//    	builder.mode(Mode.SEARCH);

//      // Extendsモード
//      builder.mode(Mode.EXTENDED);

		// ユーザ辞書を読み込む場合はここでファイルパスなどを設定する
		// builder.userDictionary(filename);

    	tokenizer = builder.build();


		String parseWord = "（ニュース・タイム）埼玉、群馬両県の総菜販売店でポテトサラダなどを購入した人が腸管出血性大腸菌Ｏ１５７に感染した問題で、両県を含め、関西など計１１都県の患者から同じ遺伝子型のＯ１５７が検出されたことが、厚生労働省への取材でわかった。同省によると、感染源が特定できないまま、同じ型の菌が広がるのは異例の事態という。";
		List<Token> tokenNormal = tokenizer.tokenize(parseWord);
		for (Token token : tokenNormal) {
			// getSurface()で分解した単語を取得できる
			// getAllFeatures()で分解した単語の品詞を取得できる
			System.out.println(token.getSurface() + " : " + token.getAllFeatures());
		}
		System.out.println("----------------------------------------------------------");

		String[] nouns_array = extract(parseWord);
		String nouns = Arrays.stream(nouns_array).collect(Collectors.joining("\n"));
		System.out.println(nouns);
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor)
	{
	    Map<Object, Boolean> map = new ConcurrentHashMap<>();
	    return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	public static String[] extract(String text) {
		List<Token> tokens = tokenizer.tokenize(text);

		// 品詞の文字列にに名詞が含まれる場合で除外した品詞を列挙
		List<String> ng_part_of_speech = Arrays.asList("引用文字列", "動詞非自立的", "接続詞的", "人名", "形容動詞語幹", "助動詞語幹", "特殊", "接尾副詞可能", "代名詞", "非自立");

		List<Token> noun_tokens = tokens.stream()
				// 重複削除
				.filter(distinctByKey(token -> token.getSurface()))
			    .filter(token -> {
			    		String pos1 = token.getPartOfSpeechLevel1();
			    		String pos_detail = token.getPartOfSpeechLevel2() + "," + token.getPartOfSpeechLevel3() + "," + token.getPartOfSpeechLevel4();
			    		if(ng_part_of_speech.stream().anyMatch(ng -> (pos1 + pos_detail).contains(ng))) return false;
			    		return Arrays.asList(pos1.split(",", 0)).contains("名詞");
			    	})
			    .filter(token -> {
					// 抽出した名詞が全角・半角記号のみで構成されている場合は除外する
					if (token == null) return false;
					return !token.getSurface().replaceAll(SYMBOL_REGEX, "").isEmpty();
				})
			    .collect(Collectors.toList());

		return noun_tokens.stream().map(noun_token -> noun_token.getSurface()).toArray(String[]::new);
	}

}
