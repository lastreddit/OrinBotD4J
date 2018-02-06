package com.github.xhiroyui.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.xhiroyui.bean.GBFCharacter;
import com.google.gson.JsonParser;

public class GBFWikiParser {
	final static String baseJsonUrl = "https://gbf.wiki/api.php?action=query&prop=revisions&rvprop=content&format=json&formatversion=2&titles=";
	final static String baseSearchUrl = "https://gbf.wiki/index.php?search=";
	private Matcher matcher; 
	
	public String gbfWikiSearch(String[] query) {
		String searchUrl = baseSearchUrl + query[0];
		for (int i = 1; i < query.length; i++) {
				searchUrl = searchUrl + "+" + query[i];
		}
		return searchUrl;
	}

	public String[] nameSplitter(String message) {
		String[] name = StringUtils.split(message.toLowerCase(), ' ');
		return name;
	}
	
	public GBFCharacter parseGbfCharacterOld(String url) throws IOException {
		GBFCharacter character = new GBFCharacter();
		Document doc = Jsoup.connect(url).get();
		character.setBaseUri(doc.baseUri());
		
		Element content = doc.getElementById("content");
		
		Elements h1 = content.select("h1#firstHeading");
		character.setName(h1.text());

		Elements baseArt = content.select("[title=\"Base Art\"] img[src]");	
		character.setImageUrl(baseArt.attr("abs:src"));
		
		Elements thumbnail = content.select("div#mw-content-text a[title=\""+h1.text()+"\"] img");		
		character.setThumbnailUrl(thumbnail.attr("abs:src"));
		
//		Elements spriteArt = content.select("[title$=\"Sprite\"] img[src]");	
//		for (Element sprites : spriteArt) {
//			character.setThumbnailUrl(sprites.attr("abs:src"));
//		}
		
		Elements table = content.select("[class^=\"wikitable character\"]");
		for (Element tablerow : table) {
			Elements tablecontents = tablerow.select("tr");
			for (Element trcontents : tablecontents) {
				if (trcontents.text().toLowerCase().contains("element".toLowerCase())) {
					Elements td = trcontents.select("img");
					if (td.toString().toLowerCase().contains("label".toLowerCase())) {
						character.setElement(td.attr("alt").substring(14, td.attr("alt").length() - 4));
					}
				}
				if (trcontents.text().toLowerCase().contains("race".toLowerCase())) {
					Elements td = trcontents.select("img");
					if (td.toString().toLowerCase().contains("label".toLowerCase())) {
						character.setRace(td.attr("alt").substring(11, td.attr("alt").length() - 4));
					}
				}
				if (trcontents.text().toLowerCase().contains("style".toLowerCase())) {
					Elements td = trcontents.select("img");
					if (td.toString().toLowerCase().contains("label".toLowerCase())) {
						character.setStyle(td.attr("alt").substring(11, td.attr("alt").length() - 4));
					}
				}
				if (trcontents.text().toLowerCase().contains("gender".toLowerCase())) {
					Elements td = trcontents.select("td");
					character.setGender(td.text());
				}
				if (trcontents.text().toLowerCase().contains("specialty".toLowerCase())) {
					Elements td = trcontents.select("img");
					if (td.toString().toLowerCase().contains("label".toLowerCase())) {
//						character.getSpecialty().add(td.attr("alt").substring(13, td.attr("alt").length() - 4));
					}
				}
			}

		}
		return character;
	}

	public GBFCharacter parseGbfCharacter(String url) throws IOException {
		// Web part
		GBFCharacter character = new GBFCharacter();
//		System.out.println("Starting jsoup connect : " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
		Document doc = Jsoup.connect(url).get();
//		System.out.println("Jsoup received : " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
		character.setBaseUri(doc.baseUri());
		character.setThumbnailUrl(doc.select("meta[property=\"og:image\"]").attr("content"));
		character.setDescription(doc.select("meta[name=\"description\"]").attr("content"));
		Element content = doc.getElementById("content");
		character.setImageUrl(content.select("[title=\"Base Art\"] img[src]").attr("abs:src"));
		character.setRarityImageUrl(content.select("img[alt^=\"Rarity\"]").attr("abs:src"));
		character.setVoiceActor(new String[] {doc.select("a.extiw").text(), doc.select("a.extiw").attr("abs:href")});
		
		// Json Part
		URL jsonurl = new URL(baseJsonUrl + URLEncoder.encode(doc.select("h1#firstHeading").text(), "UTF-8"));
		InputStreamReader reader = new InputStreamReader(jsonurl.openStream());
		JsonParser jsonParser = new JsonParser();
		String jsonContent = jsonParser.parse(reader).getAsJsonObject().get("query").getAsJsonObject().get("pages")
				.getAsJsonArray().get(0).getAsJsonObject().get("revisions").getAsJsonArray().get(0).getAsJsonObject()
				.get("content").getAsString();
		String[] values = jsonContent.split("\\\n\\|");
		for (String contents : values) {
			if (contents.toLowerCase().startsWith("id"))
				character.setId(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("name"))
				character.setName(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("releaseDate"))
				character.setReleaseDate(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("gender"))
				character.setGender(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("title"))
				character.setTitle(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("rarity"))
				character.setRarity(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("element"))
				character.setElement(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("type"))
				character.setStyle(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("race"))
				character.setRace(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("join=")) {
				matcher = RegEx.joinPattern.matcher(contents.substring(contents.lastIndexOf("=") + 1).trim());
				if (matcher.find()) {
					character.setObtainableFrom(new String[] { matcher.group(1), doc.select("a[title=\""+ matcher.group(1) +"\"]").attr("abs:href")});
				}
			}
//			 else if (contents.toLowerCase().startsWith("recruitmentWeapon"))
			// character.setRecruitmentWeapon(contents.substring(contents.lastIndexOf("=")
			// + 1).trim());
			else if (contents.toLowerCase().startsWith("weapon"))
				character.setSpecialty(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("min_atk"))
				character.setMinAtk(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("max_atk"))
				character.setMaxAtk(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("flb_atk"))
				character.setFlbAtk(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("bonus_atk"))
				character.setBonusAtk(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("min_hp"))
				character.setMinHp(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("max_hp"))
				character.setMaxHp(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("flb_hp"))
				character.setFlbHp(contents.substring(contents.lastIndexOf("=") + 1).trim());
			else if (contents.toLowerCase().startsWith("bonus_hp"))
				character.setBonusHp(contents.substring(contents.lastIndexOf("=") + 1).trim());
		}
		return character;
	}
}