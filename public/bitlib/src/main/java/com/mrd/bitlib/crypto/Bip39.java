/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mrd.bitlib.crypto;

import com.google.common.base.Optional;
import com.lambdaworks.crypto.PBKDF;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of Bip39
 */
public class Bip39 {

   private static final String ALGORITHM = "HmacSHA512";
   private static final int REPETITIONS = 2048;
   private static final int BIP32_SEED_LENGTH = 64;
   private static final String BASE_SALT = "mnemonic";
   private static final String UTF8 = "UTF-8";
   public static final byte ENGLISH_WORD_LIST_TYPE = 0;

   public static class MasterSeed implements Serializable {
      private static final long serialVersionUID = 1L;

      private final byte[] _bip39RawEntropy;
      private final String _bip39Passphrase;
      private final byte[] _bip32MasterSeed;
      private final byte _wordListType;


      private MasterSeed(byte[] bip39RawEntropy, String bip39Passphrase, byte[] bip32MasterSeed) {
         _bip39RawEntropy = bip39RawEntropy;
         _bip39Passphrase = bip39Passphrase;
         _bip32MasterSeed = bip32MasterSeed;
         _wordListType = ENGLISH_WORD_LIST_TYPE;
      }

      public byte[] getBip39RawEntropy() {
         return _bip39RawEntropy;
      }

      public List<String> getBip39WordList() {
         return Arrays.asList(rawEntropyToWords(_bip39RawEntropy));
      }

      public String getBip39Passphrase() {
         return _bip39Passphrase;
      }

      public byte[] getBip32Seed() {
         return BitUtils.copyByteArray(_bip32MasterSeed);
      }

      public byte getWordListType() {
         return _wordListType;
      }

      /**
       * Turn the master seed into binary form. The format can be full or compressed. Compressed form is smaller,
       * but requires the BIP32 seed to be calculated.
       *
       * @param compressed Use the compressed form or the full form
       * @return the master seed in binary form
       */
      public byte[] toBytes(boolean compressed) {
         ByteWriter writer = new ByteWriter(1024);

         // Add the word list type used
         writer.put(_wordListType);

         // Add the raw entropy
         putByteArray(_bip39RawEntropy, writer);

         // Add the passphrase
         try {
            putByteArray(_bip39Passphrase.getBytes("UTF-8"), writer);
         } catch (UnsupportedEncodingException e) {
            // Never happens
            throw new RuntimeException(e);
         }

         // The uncompressed form also has the BIP32 seed
         if (!compressed) {
            putByteArray(_bip32MasterSeed, writer);
         }
         return writer.toBytes();
      }

      /**
       * Creates a MasterSeed from binary form
       *
       * @param bytes      the binary form
       * @param compressed is the binary form a compressed or uncompressed representation
       * @return A master seed
       */
      public static Optional<MasterSeed> fromBytes(byte[] bytes, boolean compressed) {
         ByteReader reader = new ByteReader(bytes);
         try {
            // Get the type of word list used. So far only english is supported
            byte wordListType = reader.get();
            if (wordListType != ENGLISH_WORD_LIST_TYPE) {
               return Optional.absent();
            }
            byte[] bip39RawEntropy = getByteArray(reader);
            if (bip39RawEntropy.length != 128 / 8 &&
                  bip39RawEntropy.length != 160 / 8 &&
                  bip39RawEntropy.length != 192 / 8 &&
                  bip39RawEntropy.length != 224 / 8 &&
                  bip39RawEntropy.length != 256 / 8) {
               return Optional.absent();
            }
            String bip39Passphrase;
            try {
               byte[] bip39PassphraseBytes = getByteArray(reader);
               bip39Passphrase = new String(bip39PassphraseBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
               // Never happens
               throw new RuntimeException(e);
            }

            if (compressed) {
               // We are using compressed form, so we have to calculate the actual master seed
               return Optional.of(generateSeedFromWordList(rawEntropyToWords(bip39RawEntropy), bip39Passphrase));
            } else {
               byte[] bip32MasterSeed = getByteArray(reader);
               if (bip32MasterSeed.length != BIP32_SEED_LENGTH) {
                  return Optional.absent();
               }
               return Optional.of(new MasterSeed(bip39RawEntropy, bip39Passphrase, bip32MasterSeed));
            }
         } catch (ByteReader.InsufficientBytesException e) {
            return Optional.absent();
         }
      }

      private static byte[] getByteArray(ByteReader reader) throws ByteReader.InsufficientBytesException {
         int size = (int) reader.getCompactInt();
         if (size < 0 || size > 200) {
            throw new ByteReader.InsufficientBytesException();
         }
         return reader.getBytes(size);
      }

      private static void putByteArray(byte[] buf, ByteWriter writer) {
         writer.putCompactInt(buf.length);
         writer.putBytes(buf);
      }

      @Override
      public int hashCode() {
         return (int) BitUtils.uint32ToLong(_bip32MasterSeed, 0);
      }

      @Override
      public boolean equals(Object obj) {
         if (!(obj instanceof MasterSeed)) {
            return false;
         }
         MasterSeed other = (MasterSeed) obj;
         return BitUtils.areEqual(_bip32MasterSeed, other._bip32MasterSeed);
      }
   }


   /**
    * Check whether a list of words is valid
    * </p>
    * Checks that the number of words is valid for Bip39.
    * Checks that the words are in the list of accepted words.
    * Checks that the word have a valid checksum.
    *
    * @param wordList
    * @return
    */
   public static boolean isValidWordList(String[] wordList) {
      // Check word list length
      if (wordList.length != 12 &&
            wordList.length != 15 &&
            wordList.length != 18 &&
            wordList.length != 21 &&
            wordList.length != 24) {
         return false;
      }

      // Check words
      int bitLength = wordList.length * 11;
      for (int i = 0; i < wordList.length; i++) {
         String word = wordList[i];
         int wordIndex = getWordIndex(word);
         if (wordIndex == -1) {
            return false;
         }
      }

      // Get bytes
      byte[] rawAndChecksum = wordListToBytes(wordList);

      // Verify checksum
      return verifyChecksum(rawAndChecksum);
   }

   private static boolean verifyChecksum(byte[] rawAndChecksum) {
      int bitLength = rawAndChecksum.length * 8;
      int checksumLength;
      if (bitLength == 128 + 8) {
         checksumLength = 4;
      } else if (bitLength == 160 + 8) {
         checksumLength = 5;
      } else if (bitLength == 192 + 8) {
         checksumLength = 6;
      } else if (bitLength == 224 + 8) {
         checksumLength = 7;
      } else if (bitLength == 256 + 8) {
         checksumLength = 8;
      } else {
         return false;
      }

      // Get the raw entropy
      byte[] raw = new byte[rawAndChecksum.length - 1];
      System.arraycopy(rawAndChecksum, 0, raw, 0, raw.length);

      // Calculate checksum
      byte[] csHash = HashUtils.sha256(raw).getBytes();
      byte checksumByte = (byte) (((0xFF << (8 - checksumLength)) & 0xFF) & (0xFF & ((int) csHash[0])));

      // Verify that the checksum is valid
      byte c = rawAndChecksum[rawAndChecksum.length - 1];
      return checksumByte == c;
   }

   /**
    * Checks that a word is a valid word in the english word list for BIP39
    *
    * @param word the word to check
    * @return true if the word is a valid word in the english word list for BIP39
    */
   public static boolean isValidWord(String word) {
      return getWordIndex(word) != -1;
   }

   private static byte[] wordListToBytes(String[] wordList) {
      if (wordList.length != 12 &&
            wordList.length != 15 &&
            wordList.length != 18 &&
            wordList.length != 21 &&
            wordList.length != 24) {
         throw new RuntimeException("Word list must be 12, 15, 18, 21, or 24 words and not " + wordList.length);

      }
      int bitLength = wordList.length * 11;
      byte[] buf = new byte[bitLength / 8 + ((bitLength % 8) > 0 ? 1 : 0)];
      for (int i = 0; i < wordList.length; i++) {
         String word = wordList[i];
         int wordIndex = getWordIndex(word);
         if (wordIndex == -1) {
            throw new RuntimeException("The word '" + word + "' is not valid");
         }
         integerTo11Bits(buf, i * 11, wordIndex);
      }
      return buf;
   }

   private static byte[] wordListToRawEntropy(String[] wordList) {
      // Get the bytes of the word list
      byte[] bytes = wordListToBytes(wordList);
      // Chop off the checksum byte
      return BitUtils.copyOf(bytes, bytes.length - 1);
   }

   private static void integerTo11Bits(byte[] buf, int bitIndex, int integer) {
      for (int i = 0; i < 11; i++) {
         if ((integer & 0x400) == 0x400) {
            setBit(buf, bitIndex + i);
         }
         integer = integer << 1;
      }
   }

   private static void setBit(byte[] buf, int bitIndex) {
      int value = ((int) buf[bitIndex / 8]) & 0xFF;
      value = value | (1 << (7 - (bitIndex % 8)));
      buf[bitIndex / 8] = (byte) value;
   }

   private static int getWordIndex(String word) {
      for (int i = 0; i < ENGLISH_WORD_LIST.length; i++) {
         if (ENGLISH_WORD_LIST[i].equals(word)) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Turn raw entropy into a BIP39 word list with checksum.
    * <p/>
    * The raw entropy must be 128, 160, 192, 244, or 256 bits
    *
    * @param rawEntropy the raw entropy
    * @return the corresponding list of words
    */
   public static String[] rawEntropyToWords(byte[] rawEntropy) {
      int bitLength = rawEntropy.length * 8;
      if (bitLength != 128 &&
            bitLength != 160 &&
            bitLength != 192 &&
            bitLength != 224 &&
            bitLength != 256) {
         throw new RuntimeException("Raw entropy must be 128, 160, 192, 224, or 256 bits and not " + bitLength);
      }

      // Calculate the checksum
      int checksumLength = bitLength / 32;
      byte[] csHash = HashUtils.sha256(rawEntropy).getBytes();
      byte checksumByte = (byte) (((0xFF << (8 - checksumLength)) & 0xFF) & (0xFF & ((int) csHash[0])));

      // Append the checksum to the raw entropy
      byte[] buf = new byte[rawEntropy.length + 1];
      System.arraycopy(rawEntropy, 0, buf, 0, rawEntropy.length);
      buf[rawEntropy.length] = checksumByte;

      // Turn the array of bytes into a word list where each word represents 11 bits
      String[] words = new String[(bitLength + checksumLength) / 11];
      for (int i = 0; i < words.length; i++) {
         int wordIndex = integerFrom11Bits(buf, i * 11);
         words[i] = ENGLISH_WORD_LIST[wordIndex];
      }
      return words;
   }

   /**
    * Turn the next 11 bits of a specified bit index of an array of bytes into a positive integer
    *
    * @param buf      the array of bytes
    * @param bitIndex the bit index in the array of bytes
    * @return the next 11 bits of a specified bit index of an array of bytes into a positive integer
    */
   private static int integerFrom11Bits(byte[] buf, int bitIndex) {
      int value = 0;
      for (int i = 0; i < 11; i++) {
         if (isBitSet(buf, bitIndex + i)) {
            value = (value << 1) | 0x01;
         } else {
            value = (value << 1);
         }
      }
      return value;
   }

   /**
    * Determine whether s bit at a specified index in an array of bytes is set.
    *
    * @param buf      the array of bytes
    * @param bitIndex the bit index in the array of bytes
    * @return true if the bit is set, false otherwise
    */
   private static boolean isBitSet(byte[] buf, int bitIndex) {
      int val = ((int) buf[bitIndex / 8]) & 0xFF;
      val = val << (bitIndex % 8);
      val = val & 0x80;
      return val == 0x80;
   }

   /**
    * Create a random master seed from a random source
    *
    * @param randomSource the random source to use
    * @return a random master seed
    */
   public static MasterSeed createRandomMasterSeed(RandomSource randomSource) {
      byte[] rawEntropy = new byte[128 / 8];
      randomSource.nextBytes(rawEntropy);
      String[] wordList = rawEntropyToWords(rawEntropy);
      return generateSeedFromWordList(wordList, "");
   }

   /**
    * Generate a master seed from a BIP39 word list.
    * <p/>
    * This method does not check whether the check sum of the word list id valid
    *
    * @param wordList the word list
    * @param passphrase the optional passphrase
    * @return the BIP32 master seed
    */
   public static MasterSeed generateSeedFromWordList(String[] wordList, String passphrase) {
      return generateSeedFromWordList(new ArrayList<String>(Arrays.asList(wordList)), passphrase);
   }

   /**
    * Generate a master seed from a BIP39 word list.
    * <p/>
    * This method does not check whether the check sum of the word list id valid
    *
    * @param wordList the word list
    * @param passphrase the optional passphrase
    * @return the BIP32 master seed
    */
   public static MasterSeed generateSeedFromWordList(List<String> wordList, String passphrase) {
      // Null passphrase defaults to the empty string
      if (passphrase == null) {
         passphrase = "";
      }

      // Concatenate all words using a single space as separator
      StringBuilder sb = new StringBuilder();
      for (String s : wordList) {
         sb.append(s).append(' ');
      }
      String mnemonic = sb.toString().trim();

      // The salt is is the passphrase with a prefix
      String salt = BASE_SALT + passphrase;

      // Calculate and return the seed
      byte[] seed;
      try {
         seed = PBKDF.pbkdf2(ALGORITHM, mnemonic.getBytes(UTF8), salt.getBytes(UTF8), REPETITIONS, BIP32_SEED_LENGTH);
      } catch (UnsupportedEncodingException e) {
         // UTF-8 should be supported by every system we run on
         throw new RuntimeException(e);
      } catch (GeneralSecurityException e) {
         // HMAC-SHA512 should be supported by every system we run on
         throw new RuntimeException(e);
      }
      MasterSeed masterSeed = new MasterSeed(wordListToRawEntropy(wordList.toArray(new String[0])), passphrase, seed);
      return masterSeed;
   }


   public static final String[] ENGLISH_WORD_LIST = new String[]{"abandon", "ability", "able", "about", "above",
         "absent", "absorb", "abstract", "absurd", "abuse", "access", "accident", "account", "accuse", "achieve",
         "acid", "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual", "adapt", "add",
         "addict", "address", "adjust", "admit", "adult", "advance", "advice", "aerobic", "affair", "afford", "afraid",
         "again", "age", "agent", "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album", "alcohol",
         "alert", "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already", "also", "alter", "always",
         "amateur", "amazing", "among", "amount", "amused", "analyst", "anchor", "ancient", "anger", "angle", "angry",
         "animal", "ankle", "announce", "annual", "another", "answer", "antenna", "antique", "anxiety", "any", "apart",
         "apology", "appear", "apple", "approve", "april", "arch", "arctic", "area", "arena", "argue", "arm", "armed",
         "armor", "army", "around", "arrange", "arrest", "arrive", "arrow", "art", "artefact", "artist", "artwork",
         "ask", "aspect", "assault", "asset", "assist", "assume", "asthma", "athlete", "atom", "attack", "attend",
         "attitude", "attract", "auction", "audit", "august", "aunt", "author", "auto", "autumn", "average", "avocado",
         "avoid", "awake", "aware", "away", "awesome", "awful", "awkward", "axis", "baby", "bachelor", "bacon",
         "badge", "bag", "balance", "balcony", "ball", "bamboo", "banana", "banner", "bar", "barely", "bargain",
         "barrel", "base", "basic", "basket", "battle", "beach", "bean", "beauty", "because", "become", "beef",
         "before", "begin", "behave", "behind", "believe", "below", "belt", "bench", "benefit", "best", "betray",
         "better", "between", "beyond", "bicycle", "bid", "bike", "bind", "biology", "bird", "birth", "bitter",
         "black", "blade", "blame", "blanket", "blast", "bleak", "bless", "blind", "blood", "blossom", "blouse",
         "blue", "blur", "blush", "board", "boat", "body", "boil", "bomb", "bone", "bonus", "book", "boost", "border",
         "boring", "borrow", "boss", "bottom", "bounce", "box", "boy", "bracket", "brain", "brand", "brass", "brave",
         "bread", "breeze", "brick", "bridge", "brief", "bright", "bring", "brisk", "broccoli", "broken", "bronze",
         "broom", "brother", "brown", "brush", "bubble", "buddy", "budget", "buffalo", "build", "bulb", "bulk",
         "bullet", "bundle", "bunker", "burden", "burger", "burst", "bus", "business", "busy", "butter", "buyer",
         "buzz", "cabbage", "cabin", "cable", "cactus", "cage", "cake", "call", "calm", "camera", "camp", "can",
         "canal", "cancel", "candy", "cannon", "canoe", "canvas", "canyon", "capable", "capital", "captain", "car",
         "carbon", "card", "cargo", "carpet", "carry", "cart", "case", "cash", "casino", "castle", "casual", "cat",
         "catalog", "catch", "category", "cattle", "caught", "cause", "caution", "cave", "ceiling", "celery", "cement",
         "census", "century", "cereal", "certain", "chair", "chalk", "champion", "change", "chaos", "chapter",
         "charge", "chase", "chat", "cheap", "check", "cheese", "chef", "cherry", "chest", "chicken", "chief", "child",
         "chimney", "choice", "choose", "chronic", "chuckle", "chunk", "churn", "cigar", "cinnamon", "circle",
         "citizen", "city", "civil", "claim", "clap", "clarify", "claw", "clay", "clean", "clerk", "clever", "click",
         "client", "cliff", "climb", "clinic", "clip", "clock", "clog", "close", "cloth", "cloud", "clown", "club",
         "clump", "cluster", "clutch", "coach", "coast", "coconut", "code", "coffee", "coil", "coin", "collect",
         "color", "column", "combine", "come", "comfort", "comic", "common", "company", "concert", "conduct",
         "confirm", "congress", "connect", "consider", "control", "convince", "cook", "cool", "copper", "copy",
         "coral", "core", "corn", "correct", "cost", "cotton", "couch", "country", "couple", "course", "cousin",
         "cover", "coyote", "crack", "cradle", "craft", "cram", "crane", "crash", "crater", "crawl", "crazy", "cream",
         "credit", "creek", "crew", "cricket", "crime", "crisp", "critic", "crop", "cross", "crouch", "crowd",
         "crucial", "cruel", "cruise", "crumble", "crunch", "crush", "cry", "crystal", "cube", "culture", "cup",
         "cupboard", "curious", "current", "curtain", "curve", "cushion", "custom", "cute", "cycle", "dad", "damage",
         "damp", "dance", "danger", "daring", "dash", "daughter", "dawn", "day", "deal", "debate", "debris", "decade",
         "december", "decide", "decline", "decorate", "decrease", "deer", "defense", "define", "defy", "degree",
         "delay", "deliver", "demand", "demise", "denial", "dentist", "deny", "depart", "depend", "deposit", "depth",
         "deputy", "derive", "describe", "desert", "design", "desk", "despair", "destroy", "detail", "detect",
         "develop", "device", "devote", "diagram", "dial", "diamond", "diary", "dice", "diesel", "diet", "differ",
         "digital", "dignity", "dilemma", "dinner", "dinosaur", "direct", "dirt", "disagree", "discover", "disease",
         "dish", "dismiss", "disorder", "display", "distance", "divert", "divide", "divorce", "dizzy", "doctor",
         "document", "dog", "doll", "dolphin", "domain", "donate", "donkey", "donor", "door", "dose", "double", "dove",
         "draft", "dragon", "drama", "drastic", "draw", "dream", "dress", "drift", "drill", "drink", "drip", "drive",
         "drop", "drum", "dry", "duck", "dumb", "dune", "during", "dust", "dutch", "duty", "dwarf", "dynamic", "eager",
         "eagle", "early", "earn", "earth", "easily", "east", "easy", "echo", "ecology", "economy", "edge", "edit",
         "educate", "effort", "egg", "eight", "either", "elbow", "elder", "electric", "elegant", "element", "elephant",
         "elevator", "elite", "else", "embark", "embody", "embrace", "emerge", "emotion", "employ", "empower", "empty",
         "enable", "enact", "end", "endless", "endorse", "enemy", "energy", "enforce", "engage", "engine", "enhance",
         "enjoy", "enlist", "enough", "enrich", "enroll", "ensure", "enter", "entire", "entry", "envelope", "episode",
         "equal", "equip", "era", "erase", "erode", "erosion", "error", "erupt", "escape", "essay", "essence",
         "estate", "eternal", "ethics", "evidence", "evil", "evoke", "evolve", "exact", "example", "excess",
         "exchange", "excite", "exclude", "excuse", "execute", "exercise", "exhaust", "exhibit", "exile", "exist",
         "exit", "exotic", "expand", "expect", "expire", "explain", "expose", "express", "extend", "extra", "eye",
         "eyebrow", "fabric", "face", "faculty", "fade", "faint", "faith", "fall", "false", "fame", "family", "famous",
         "fan", "fancy", "fantasy", "farm", "fashion", "fat", "fatal", "father", "fatigue", "fault", "favorite",
         "feature", "february", "federal", "fee", "feed", "feel", "female", "fence", "festival", "fetch", "fever",
         "few", "fiber", "fiction", "field", "figure", "file", "film", "filter", "final", "find", "fine", "finger",
         "finish", "fire", "firm", "first", "fiscal", "fish", "fit", "fitness", "fix", "flag", "flame", "flash",
         "flat", "flavor", "flee", "flight", "flip", "float", "flock", "floor", "flower", "fluid", "flush", "fly",
         "foam", "focus", "fog", "foil", "fold", "follow", "food", "foot", "force", "forest", "forget", "fork",
         "fortune", "forum", "forward", "fossil", "foster", "found", "fox", "fragile", "frame", "frequent", "fresh",
         "friend", "fringe", "frog", "front", "frost", "frown", "frozen", "fruit", "fuel", "fun", "funny", "furnace",
         "fury", "future", "gadget", "gain", "galaxy", "gallery", "game", "gap", "garage", "garbage", "garden",
         "garlic", "garment", "gas", "gasp", "gate", "gather", "gauge", "gaze", "general", "genius", "genre", "gentle",
         "genuine", "gesture", "ghost", "giant", "gift", "giggle", "ginger", "giraffe", "girl", "give", "glad",
         "glance", "glare", "glass", "glide", "glimpse", "globe", "gloom", "glory", "glove", "glow", "glue", "goat",
         "goddess", "gold", "good", "goose", "gorilla", "gospel", "gossip", "govern", "gown", "grab", "grace", "grain",
         "grant", "grape", "grass", "gravity", "great", "green", "grid", "grief", "grit", "grocery", "group", "grow",
         "grunt", "guard", "guess", "guide", "guilt", "guitar", "gun", "gym", "habit", "hair", "half", "hammer",
         "hamster", "hand", "happy", "harbor", "hard", "harsh", "harvest", "hat", "have", "hawk", "hazard", "head",
         "health", "heart", "heavy", "hedgehog", "height", "hello", "helmet", "help", "hen", "hero", "hidden", "high",
         "hill", "hint", "hip", "hire", "history", "hobby", "hockey", "hold", "hole", "holiday", "hollow", "home",
         "honey", "hood", "hope", "horn", "horror", "horse", "hospital", "host", "hotel", "hour", "hover", "hub",
         "huge", "human", "humble", "humor", "hundred", "hungry", "hunt", "hurdle", "hurry", "hurt", "husband",
         "hybrid", "ice", "icon", "idea", "identify", "idle", "ignore", "ill", "illegal", "illness", "image",
         "imitate", "immense", "immune", "impact", "impose", "improve", "impulse", "inch", "include", "income",
         "increase", "index", "indicate", "indoor", "industry", "infant", "inflict", "inform", "inhale", "inherit",
         "initial", "inject", "injury", "inmate", "inner", "innocent", "input", "inquiry", "insane", "insect",
         "inside", "inspire", "install", "intact", "interest", "into", "invest", "invite", "involve", "iron", "island",
         "isolate", "issue", "item", "ivory", "jacket", "jaguar", "jar", "jazz", "jealous", "jeans", "jelly", "jewel",
         "job", "join", "joke", "journey", "joy", "judge", "juice", "jump", "jungle", "junior", "junk", "just",
         "kangaroo", "keen", "keep", "ketchup", "key", "kick", "kid", "kidney", "kind", "kingdom", "kiss", "kit",
         "kitchen", "kite", "kitten", "kiwi", "knee", "knife", "knock", "know", "lab", "label", "labor", "ladder",
         "lady", "lake", "lamp", "language", "laptop", "large", "later", "latin", "laugh", "laundry", "lava", "law",
         "lawn", "lawsuit", "layer", "lazy", "leader", "leaf", "learn", "leave", "lecture", "left", "leg", "legal",
         "legend", "leisure", "lemon", "lend", "length", "lens", "leopard", "lesson", "letter", "level", "liar",
         "liberty", "library", "license", "life", "lift", "light", "like", "limb", "limit", "link", "lion", "liquid",
         "list", "little", "live", "lizard", "load", "loan", "lobster", "local", "lock", "logic", "lonely", "long",
         "loop", "lottery", "loud", "lounge", "love", "loyal", "lucky", "luggage", "lumber", "lunar", "lunch",
         "luxury", "lyrics", "machine", "mad", "magic", "magnet", "maid", "mail", "main", "major", "make", "mammal",
         "man", "manage", "mandate", "mango", "mansion", "manual", "maple", "marble", "march", "margin", "marine",
         "market", "marriage", "mask", "mass", "master", "match", "material", "math", "matrix", "matter", "maximum",
         "maze", "meadow", "mean", "measure", "meat", "mechanic", "medal", "media", "melody", "melt", "member",
         "memory", "mention", "menu", "mercy", "merge", "merit", "merry", "mesh", "message", "metal", "method",
         "middle", "midnight", "milk", "million", "mimic", "mind", "minimum", "minor", "minute", "miracle", "mirror",
         "misery", "miss", "mistake", "mix", "mixed", "mixture", "mobile", "model", "modify", "mom", "moment",
         "monitor", "monkey", "monster", "month", "moon", "moral", "more", "morning", "mosquito", "mother", "motion",
         "motor", "mountain", "mouse", "move", "movie", "much", "muffin", "mule", "multiply", "muscle", "museum",
         "mushroom", "music", "must", "mutual", "myself", "mystery", "myth", "naive", "name", "napkin", "narrow",
         "nasty", "nation", "nature", "near", "neck", "need", "negative", "neglect", "neither", "nephew", "nerve",
         "nest", "net", "network", "neutral", "never", "news", "next", "nice", "night", "noble", "noise", "nominee",
         "noodle", "normal", "north", "nose", "notable", "note", "nothing", "notice", "novel", "now", "nuclear",
         "number", "nurse", "nut", "oak", "obey", "object", "oblige", "obscure", "observe", "obtain", "obvious",
         "occur", "ocean", "october", "odor", "off", "offer", "office", "often", "oil", "okay", "old", "olive",
         "olympic", "omit", "once", "one", "onion", "online", "only", "open", "opera", "opinion", "oppose", "option",
         "orange", "orbit", "orchard", "order", "ordinary", "organ", "orient", "original", "orphan", "ostrich",
         "other", "outdoor", "outer", "output", "outside", "oval", "oven", "over", "own", "owner", "oxygen", "oyster",
         "ozone", "pact", "paddle", "page", "pair", "palace", "palm", "panda", "panel", "panic", "panther", "paper",
         "parade", "parent", "park", "parrot", "party", "pass", "patch", "path", "patient", "patrol", "pattern",
         "pause", "pave", "payment", "peace", "peanut", "pear", "peasant", "pelican", "pen", "penalty", "pencil",
         "people", "pepper", "perfect", "permit", "person", "pet", "phone", "photo", "phrase", "physical", "piano",
         "picnic", "picture", "piece", "pig", "pigeon", "pill", "pilot", "pink", "pioneer", "pipe", "pistol", "pitch",
         "pizza", "place", "planet", "plastic", "plate", "play", "please", "pledge", "pluck", "plug", "plunge", "poem",
         "poet", "point", "polar", "pole", "police", "pond", "pony", "pool", "popular", "portion", "position",
         "possible", "post", "potato", "pottery", "poverty", "powder", "power", "practice", "praise", "predict",
         "prefer", "prepare", "present", "pretty", "prevent", "price", "pride", "primary", "print", "priority",
         "prison", "private", "prize", "problem", "process", "produce", "profit", "program", "project", "promote",
         "proof", "property", "prosper", "protect", "proud", "provide", "public", "pudding", "pull", "pulp", "pulse",
         "pumpkin", "punch", "pupil", "puppy", "purchase", "purity", "purpose", "purse", "push", "put", "puzzle",
         "pyramid", "quality", "quantum", "quarter", "question", "quick", "quit", "quiz", "quote", "rabbit", "raccoon",
         "race", "rack", "radar", "radio", "rail", "rain", "raise", "rally", "ramp", "ranch", "random", "range",
         "rapid", "rare", "rate", "rather", "raven", "raw", "razor", "ready", "real", "reason", "rebel", "rebuild",
         "recall", "receive", "recipe", "record", "recycle", "reduce", "reflect", "reform", "refuse", "region",
         "regret", "regular", "reject", "relax", "release", "relief", "rely", "remain", "remember", "remind", "remove",
         "render", "renew", "rent", "reopen", "repair", "repeat", "replace", "report", "require", "rescue", "resemble",
         "resist", "resource", "response", "result", "retire", "retreat", "return", "reunion", "reveal", "review",
         "reward", "rhythm", "rib", "ribbon", "rice", "rich", "ride", "ridge", "rifle", "right", "rigid", "ring",
         "riot", "ripple", "risk", "ritual", "rival", "river", "road", "roast", "robot", "robust", "rocket", "romance",
         "roof", "rookie", "room", "rose", "rotate", "rough", "round", "route", "royal", "rubber", "rude", "rug",
         "rule", "run", "runway", "rural", "sad", "saddle", "sadness", "safe", "sail", "salad", "salmon", "salon",
         "salt", "salute", "same", "sample", "sand", "satisfy", "satoshi", "sauce", "sausage", "save", "say", "scale",
         "scan", "scare", "scatter", "scene", "scheme", "school", "science", "scissors", "scorpion", "scout", "scrap",
         "screen", "script", "scrub", "sea", "search", "season", "seat", "second", "secret", "section", "security",
         "seed", "seek", "segment", "select", "sell", "seminar", "senior", "sense", "sentence", "series", "service",
         "session", "settle", "setup", "seven", "shadow", "shaft", "shallow", "share", "shed", "shell", "sheriff",
         "shield", "shift", "shine", "ship", "shiver", "shock", "shoe", "shoot", "shop", "short", "shoulder", "shove",
         "shrimp", "shrug", "shuffle", "shy", "sibling", "sick", "side", "siege", "sight", "sign", "silent", "silk",
         "silly", "silver", "similar", "simple", "since", "sing", "siren", "sister", "situate", "six", "size", "skate",
         "sketch", "ski", "skill", "skin", "skirt", "skull", "slab", "slam", "sleep", "slender", "slice", "slide",
         "slight", "slim", "slogan", "slot", "slow", "slush", "small", "smart", "smile", "smoke", "smooth", "snack",
         "snake", "snap", "sniff", "snow", "soap", "soccer", "social", "sock", "soda", "soft", "solar", "soldier",
         "solid", "solution", "solve", "someone", "song", "soon", "sorry", "sort", "soul", "sound", "soup", "source",
         "south", "space", "spare", "spatial", "spawn", "speak", "special", "speed", "spell", "spend", "sphere",
         "spice", "spider", "spike", "spin", "spirit", "split", "spoil", "sponsor", "spoon", "sport", "spot", "spray",
         "spread", "spring", "spy", "square", "squeeze", "squirrel", "stable", "stadium", "staff", "stage", "stairs",
         "stamp", "stand", "start", "state", "stay", "steak", "steel", "stem", "step", "stereo", "stick", "still",
         "sting", "stock", "stomach", "stone", "stool", "story", "stove", "strategy", "street", "strike", "strong",
         "struggle", "student", "stuff", "stumble", "style", "subject", "submit", "subway", "success", "such",
         "sudden", "suffer", "sugar", "suggest", "suit", "summer", "sun", "sunny", "sunset", "super", "supply",
         "supreme", "sure", "surface", "surge", "surprise", "surround", "survey", "suspect", "sustain", "swallow",
         "swamp", "swap", "swarm", "swear", "sweet", "swift", "swim", "swing", "switch", "sword", "symbol", "symptom",
         "syrup", "system", "table", "tackle", "tag", "tail", "talent", "talk", "tank", "tape", "target", "task",
         "taste", "tattoo", "taxi", "teach", "team", "tell", "ten", "tenant", "tennis", "tent", "term", "test", "text",
         "thank", "that", "theme", "then", "theory", "there", "they", "thing", "this", "thought", "three", "thrive",
         "throw", "thumb", "thunder", "ticket", "tide", "tiger", "tilt", "timber", "time", "tiny", "tip", "tired",
         "tissue", "title", "toast", "tobacco", "today", "toddler", "toe", "together", "toilet", "token", "tomato",
         "tomorrow", "tone", "tongue", "tonight", "tool", "tooth", "top", "topic", "topple", "torch", "tornado",
         "tortoise", "toss", "total", "tourist", "toward", "tower", "town", "toy", "track", "trade", "traffic",
         "tragic", "train", "transfer", "trap", "trash", "travel", "tray", "treat", "tree", "trend", "trial", "tribe",
         "trick", "trigger", "trim", "trip", "trophy", "trouble", "truck", "true", "truly", "trumpet", "trust",
         "truth", "try", "tube", "tuition", "tumble", "tuna", "tunnel", "turkey", "turn", "turtle", "twelve", "twenty",
         "twice", "twin", "twist", "two", "type", "typical", "ugly", "umbrella", "unable", "unaware", "uncle",
         "uncover", "under", "undo", "unfair", "unfold", "unhappy", "uniform", "unique", "unit", "universe", "unknown",
         "unlock", "until", "unusual", "unveil", "update", "upgrade", "uphold", "upon", "upper", "upset", "urban",
         "urge", "usage", "use", "used", "useful", "useless", "usual", "utility", "vacant", "vacuum", "vague", "valid",
         "valley", "valve", "van", "vanish", "vapor", "various", "vast", "vault", "vehicle", "velvet", "vendor",
         "venture", "venue", "verb", "verify", "version", "very", "vessel", "veteran", "viable", "vibrant", "vicious",
         "victory", "video", "view", "village", "vintage", "violin", "virtual", "virus", "visa", "visit", "visual",
         "vital", "vivid", "vocal", "voice", "void", "volcano", "volume", "vote", "voyage", "wage", "wagon", "wait",
         "walk", "wall", "walnut", "want", "warfare", "warm", "warrior", "wash", "wasp", "waste", "water", "wave",
         "way", "wealth", "weapon", "wear", "weasel", "weather", "web", "wedding", "weekend", "weird", "welcome",
         "west", "wet", "whale", "what", "wheat", "wheel", "when", "where", "whip", "whisper", "wide", "width", "wife",
         "wild", "will", "win", "window", "wine", "wing", "wink", "winner", "winter", "wire", "wisdom", "wise", "wish",
         "witness", "wolf", "woman", "wonder", "wood", "wool", "word", "work", "world", "worry", "worth", "wrap",
         "wreck", "wrestle", "wrist", "write", "wrong", "yard", "year", "yellow", "you", "young", "youth", "zebra",
         "zero", "zone", "zoo"};
}
