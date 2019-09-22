/**
 * Copyright (c) 2005-2018, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.awt.Color;

import java.io.File;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.chat.StyledChatBuffer;

import net.sourceforge.kolmafia.swingui.menu.PartialMRUList;
import net.sourceforge.kolmafia.swingui.menu.ScriptMRUList;

import net.sourceforge.kolmafia.utilities.LockableListFactory;

// The following are objects that go into various global data lists.
// Perhaps we should have a DataModel.java?

import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.EncounterManager.RegisteredEncounter;

public interface KoLConstants
	extends UtilityConstants
{
	// Version information for the current version of KoLmafia.
	// Rendered in various locations and therefore made public.

	public static final String VERSION_NAME = "KoLmafia v19.7";
	public static final String VERSION_DATE = "Released on July 28, 2019";
	public static final boolean RELEASED = true;
	public static final String REVISION = null;

	// General constants used for calculations and formatting of
	// strings, as well as for string parsing.

	public static final Random RNG = new Random();
	public static final Pattern LINE_BREAK_PATTERN = Pattern.compile( "\\s*[\r\n]+\\s*" );

	public static final Pattern ANYTAG_PATTERN = Pattern.compile( "<.*?>" );
	public static final Pattern ANYTAG_BUT_ITALIC_PATTERN = Pattern.compile( "<(?!i>)(?!/i>).*?>" );
	public static final Pattern SCRIPT_PATTERN = Pattern.compile( "<script.*?</script>", Pattern.DOTALL );
	public static final Pattern STYLE_PATTERN = Pattern.compile( "<style.*?</style>", Pattern.DOTALL );
	public static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--.*?-->", Pattern.DOTALL );
	public static final Pattern HEAD_PATTERN = Pattern.compile( "<head>(.*?)</head>" );
	public static final Pattern BODY_PATTERN = Pattern.compile( "<body>(.*?)</body>" );

	public static final DecimalFormat COMMA_FORMAT =
		new DecimalFormat( "#,##0;-#,##0", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat MODIFIER_FORMAT =
		new DecimalFormat( "+#0;-#0", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat SINGLE_PRECISION_FORMAT =
		new DecimalFormat( "+#,##0.0;-#,##0.0", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat FLOAT_FORMAT =
		new DecimalFormat( "#,##0.00;-#,##0.00", new DecimalFormatSymbols( Locale.US ) );
	// About 45 places needed to show the smallest denorm float.
	public static final DecimalFormat NONSCIENTIFIC_FORMAT =
		new DecimalFormat( "#0.0############################################", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat ROUNDED_MODIFIER_FORMAT =
		new DecimalFormat( "+#0.00;-#0.00", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat CHAT_LASTSEEN_FORMAT = new DecimalFormat( "0000000000" );

	public static final SimpleDateFormat DAILY_FORMAT = new SimpleDateFormat( "yyyyMMdd", Locale.US );
	public static final SimpleDateFormat WEEKLY_FORMAT = new SimpleDateFormat( "yyyyMM_'w'W", Locale.US );
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat( "HH:mm:ss z", Locale.US );

	// Generic constants which indicate null values.  Used in
	// order to preserve memory.

	public static final Class[] NOPARAMS = new Class[ 0 ];

	// Menus rendered in the relay browser and the KoLmafia mini-browser.
	// Ensures that the two menus always contain the same information.

	// Additional navigation destinations for convenience

	public static final String[][] GOTO_MENU = new String[][]
	{
		{
			"Council",
			"council.php"
		},
		{
			"Guild Hall",
			"guild.php"
		},
		{
			"Spookyraven",
			"manor.php"
		},
		{
			"Cobb's Knob",
			"cobbsknob.php"
		},
		{
			"Defiled Cyrpt",
			"crypt.php"
		},
		{
			"Mt. McLargeHuge",
			"place.php?whichplace=mclargehuge"
		},
		{
			"The Beanstalk",
			"beanstalk.php"
		},
		{
			"Elemental International Airport",
			"place.php?whichplace=airport"
		},
		{
			"The Sea",
			"thesea.php"
		},
	};

	public static final String[][] FRAME_NAMES =
	{
		{
			"Adventure",
			"AdventureFrame"
		},
		{
			"Mini-Browser",
			"RequestFrame"
		},
		{
			"Relay Server",
			"LocalRelayServer"
		},
		{
			"Purchases",
			"MallSearchFrame"
		},
		{
			"Graphical CLI",
			"CommandDisplayFrame"
		},
		{
			"Daily Deeds",
			"CharSheetFrame"
		},
		{
			"Item Manager",
			"ItemManageFrame"
		},
		{
			"Gear Changer",
			"GearChangeFrame"
		},
		{
			"Store Manager",
			"StoreManageFrame"
		},
		{
			"Coin Masters",
			"CoinmastersFrame"
		},
		{
			"Museum Display",
			"MuseumFrame"
		},
		{
			"Hall of Legends",
			"MeatManageFrame"
		},
		{
			"Skill Casting",
			"SkillBuffFrame"
		},
		{
			"Contact List",
			"ContactListFrame"
		},
		{
			"Buffbot Manager",
			"BuffBotFrame"
		},
		{
			"Purchase Buffs",
			"BuffRequestFrame"
		},
		{
			"Request a Fax",
			"FaxRequestFrame"
		},
		{
			"Mushroom Plot",
			"MushroomFrame"
		},
		{
			"Familiar Trainer",
			"FamiliarTrainingFrame"
		},
		{
			"Loathing Chat",
			"ChatManager"
		},
		{
			"Clan Management",
			"ClanManageFrame"
		},
		{
			"Farmer's Almanac",
			"CalendarFrame"
		},
		{
			"Encyclopedia",
			"DatabaseFrame"
		},
		{
			"Preferences",
			"OptionsFrame"
		},
		{
			"Sweet Synthesis",
			"SynthesizeFrame"
		},
		{
			"Modifier Maximizer",
			"MaximizerFrame"
		}
	};

	// Scripting-related constants.  Used throughout KoLmafia in
	// order to ensure proper handling of scripts.

	public static final List<File> scripts = LockableListFactory.getInstance( File.class );
	public static final List<String> bookmarks = LockableListFactory.getInstance( String.class );

	public static final ArrayList<String> disabledScripts = new ArrayList<String>();
	public static final ScriptMRUList scriptMList = 
			  new ScriptMRUList( "scriptMRUList", "scriptMRULength" );
	public static final PartialMRUList maximizerMList =
			  new PartialMRUList( "maximizerMRUList", "maximizerMRUSize", "maximizerList" );

	public static final List<AdventureResult> junkList = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> singletonList = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> mementoList = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> profitableList = LockableListFactory.getSortedInstance( AdventureResult.class );

	public static final String BUFFBOT_DIRECTORY = "buffs/";
	public static final String CCS_DIRECTORY = "ccs/";
	public static final String CHATLOG_DIRECTORY = "chats/";
	public static final String PLOTS_DIRECTORY = "planting/";
	public static final String RELAY_DIRECTORY = "relay/";
	public static final String SCRIPT_DIRECTORY = "scripts/";
	public static final String SESSIONS_DIRECTORY = "sessions/";
	public static final String SVN_DIRECTORY = "svn/";

	public static final File BUFFBOT_LOCATION =
		new File( KoLConstants.ROOT_LOCATION, KoLConstants.BUFFBOT_DIRECTORY );
	public static final File CCS_LOCATION = new File( KoLConstants.ROOT_LOCATION, KoLConstants.CCS_DIRECTORY );
	public static final File CHATLOG_LOCATION =
		new File( KoLConstants.ROOT_LOCATION, KoLConstants.CHATLOG_DIRECTORY );
	public static final File PLOTS_LOCATION = new File( KoLConstants.ROOT_LOCATION, KoLConstants.PLOTS_DIRECTORY );
	public static final File SCRIPT_LOCATION = new File( KoLConstants.ROOT_LOCATION, KoLConstants.SCRIPT_DIRECTORY );
	public static final File SESSIONS_LOCATION =
		new File( KoLConstants.ROOT_LOCATION, KoLConstants.SESSIONS_DIRECTORY );
	public static final File RELAY_LOCATION = new File( KoLConstants.ROOT_LOCATION, KoLConstants.RELAY_DIRECTORY );
	public static final File SVN_LOCATION = new File( KoLConstants.ROOT_LOCATION, KoLConstants.SVN_DIRECTORY );
	public static final File SVN_REPO_FILE = new File( KoLConstants.DATA_LOCATION, "svnrepo.json");

	// All data files that can be overridden

	public static final String[] OVERRIDE_DATA =
	{
		"adventures.txt",
		"buffbots.txt",
		"classskills.txt",
		"coinmasters.txt",
		"combats.txt",
		"concoctions.txt",
		"consequences.txt",
		"defaults.txt",
		"equipment.txt",
		"familiars.txt",
		"faxbots.txt",
		"foldgroups.txt",
		"fullness.txt",
		"inebriety.txt",
		"items.txt",
		"mallprices.txt",
		"modifiers.txt",
		"monsters.txt",
		"nonfilling.txt",
		"npcstores.txt",
		"outfits.txt",
		"packages.txt",
		"pulverize.txt",
		"spleenhit.txt",
		"statuseffects.txt",
		"zapgroups.txt",
		"zonelist.txt"
	};

	// The current version number of each data file

	public static final int ADVENTURES_VERSION = 6;
	public static final int BOUNTY_VERSION = 2;
	public static final int BUFFBOTS_VERSION = 1;
	public static final int CAFE_BOOZE_VERSION = 1;
	public static final int CAFE_FOOD_VERSION = 1;
	public static final int CLASSSKILLS_VERSION = 4;
	public static final int COINMASTERS_VERSION = 2;
	public static final int COMBATS_VERSION = 1;
	public static final int CONCOCTIONS_VERSION = 3;
	public static final int CONSEQUENCES_VERSION = 1;
	public static final int DEFAULTS_VERSION = 1;
	public static final int ENCOUNTERS_VERSION = 1;
	public static final int EQUIPMENT_VERSION = 2;
	public static final int FAMBATTLE_VERSION = 1;
	public static final int FAMILIARS_VERSION = 4;
	public static final int FAXBOTS_VERSION = 1;
	public static final int FOLDGROUPS_VERSION = 1;
	public static final int FULLNESS_VERSION = 2;
	public static final int INEBRIETY_VERSION = 2;
	public static final int ITEMS_VERSION = 1;
	public static final int MODIFIERS_VERSION = 3;
	public static final int MONSTERS_VERSION = 8;
	public static final int NONFILLING_VERSION = 1;
	public static final int NPCSTORES_VERSION = 2;
	public static final int OUTFITS_VERSION = 2;
	public static final int PACKAGES_VERSION = 1;
	public static final int PULVERIZE_VERSION = 2;
	public static final int RESTORES_VERSION = 2;
	public static final int QUESTSCOUNCIL_VERSION = 1;
	public static final int QUESTSLOG_VERSION = 1;
	public static final int SPLEENHIT_VERSION = 3;
	public static final int STATUSEFFECTS_VERSION = 3;
	public static final int ZAPGROUPS_VERSION = 1;
	public static final int ZONELIST_VERSION = 1;

	// mallprices.txt can be updated by typing a filename, so give
	// it a distinct version number in case the user types the name
	// of another one of the data files.
	public static final int MALLPRICES_VERSION = 0xF00D5;

	// The current versioned name of each KoLmafia-supplied relay file
	public static final String BARREL_SOUNDS_JS = "barrel_sounds.js";
	public static final String BASEMENT_JS = "basement.js";
	public static final String BASICS_CSS = "basics.1.css";
	public static final String BASICS_JS = "basics.js";
	public static final String CHAT_HTML = "chat.html";
	public static final String CLI_HTML = "cli.html";
	public static final String COMBATFILTER_JS = "combatfilter.1.js";
	public static final String HOTKEYS_JS = "hotkeys.js";
	public static final String IRCM_JS = "ircm_extend.js";
	public static final String MACROHELPER_JS = "macrohelper.4.js";
	public static final String ONFOCUS_JS = "onfocus.1.js";
	public static final String SORTTABLE_JS = "sorttable.2.js";
	public static final String STATIONARYBUTTONS_CSS = "stationarybuttons.2.css";
	public static final String STATIONARYBUTTONS_JS = "stationarybuttons.1.js";

	public static final String[] RELAY_FILES =
	{
		BARREL_SOUNDS_JS,
		BASEMENT_JS,
		BASICS_CSS,
		BASICS_JS,
		CHAT_HTML,
		CLI_HTML,
		COMBATFILTER_JS,
		HOTKEYS_JS,
		IRCM_JS,
		MACROHELPER_JS,
		ONFOCUS_JS,
		SORTTABLE_JS,
		STATIONARYBUTTONS_CSS,
		STATIONARYBUTTONS_JS,
	};


	// Different states of KoLmafia.  Used in order to determine
	// what is still permitted.
	public enum MafiaState {
		ENABLE,
		ERROR,
		ABORT,
		PENDING,
		CONTINUE;
	};

	public enum Stat
	{
		MUSCLE( "Muscle" ),
		MYSTICALITY( "Mysticality" ),
		MOXIE( "Moxie" ),
		SUBMUSCLE( "SubMuscle" ),
		SUBMYST( "SubMysticality" ),
		SUBMOXIE( "SubMoxie" ),
		NONE( "None" );

		private final String name;

		private Stat( String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	public enum ZodiacType
	{
		NONE,
		MUSCLE,
		MYSTICALITY,
		MOXIE,
		BAD_MOON
	}

	public enum ZodiacZone
	{
		KNOLL,
		CANADIA,
		GNOMADS,
		NONE
	}

	public enum WeaponType
	{
		MELEE,
		RANGED,
		NONE
	}

	// Mystical Book Types
	public enum BookType
	{
		TOME,
		GRIMOIRE,
		LIBRAM
	}

	// Cannot be "used" in any way by itself
	public static final int NO_CONSUME = 0;

	// Consumables
	public static final int CONSUME_EAT = 1;
	public static final int CONSUME_DRINK = 2;
	public static final int CONSUME_SPLEEN = 3;

	// Usables
	public static final int CONSUME_USE = 4;
	public static final int CONSUME_MULTIPLE = 5;
	public static final int INFINITE_USES = 6;
	public static final int MESSAGE_DISPLAY = 7;

	// Familiar hatchlings
	public static final int GROW_FAMILIAR = 8;

	// Equipment
	public static final int EQUIP_HAT = 9;
	public static final int EQUIP_WEAPON = 10;
	public static final int EQUIP_OFFHAND = 11;
	public static final int EQUIP_CONTAINER = 12;
	public static final int EQUIP_SHIRT = 13;
	public static final int EQUIP_PANTS = 14;
	public static final int EQUIP_ACCESSORY = 15;
	public static final int EQUIP_FAMILIAR = 16;

	// Customizable "equipment"
	public static final int CONSUME_STICKER = 17;
	public static final int CONSUME_CARD = 18;
	public static final int CONSUME_FOLDER = 19;
	public static final int CONSUME_BOOTSKIN = 20;
	public static final int CONSUME_BOOTSPUR = 21;
	public static final int CONSUME_SIXGUN = 22;

	// Special "uses"
	public static final int CONSUME_FOOD_HELPER = 30;
	public static final int CONSUME_DRINK_HELPER = 31;
	public static final int CONSUME_ZAP = 32;
	public static final int CONSUME_SPHERE = 33;
	public static final int CONSUME_GUARDIAN = 34;
	public static final int CONSUME_POKEPILL = 35;

	// Potions
	public static final int CONSUME_POTION = 40;
	public static final int CONSUME_AVATAR = 41;

	// Familiar "uses"
	public static final int CONSUME_ROBO = 95;
	public static final int CONSUME_MIMIC = 96;
	public static final int CONSUME_SLIME = 97;
	public static final int CONSUME_HOBO = 98;
	public static final int CONSUME_GHOST = 99;

	public enum CraftingType
	{
		SUBCLASS,	// ???
		NOCREATE,
		COMBINE,	// Items anybody can create using meat paste or The Plunger
		COOK,		// Items created with an E-Z Cook Oven or Dramatic Range
		MIX,		// Items created with a Shaker or Cocktailcrafting Kit
		SMITH,		// Items anybody can create with a tenderizing hammer or via Innabox
		SSMITH,		// Items that can only be created with a tenderizing hammer, not via Innabox
		STILL,		// Items requiring access to Nash Crosby's Still
		MALUS,		// Items requiring access to the Malus of Forethought
		JEWELRY,	// Items created with jewelry-making pliers
		STARCHART,	// Items anybody can create with starcharts, stars, and lines
		PIXEL,		// Items anybody can create with pixels
		ROLLING_PIN,	// Items created with a rolling pin or and an unrolling pin
		GNOME_TINKER,	// Items requiring access to the Gnome supertinker
		STAFF,		// Items requiring access to Roderic the Staffcrafter
		SUSHI,		// Items anybody can create with a sushi-rolling mat
		// Items created by single (or multi) using a single item.
		// Extra ingredients might also be consumed.
		// Multi-using more than one of the item creates multiple results.
		SINGLE_USE,
		// Items created by multi-using specific numbers of a single item.
		// Extra ingredients might also be consumed.
		// You must create multiple result items one at a time.
		MULTI_USE,
		SEWER,		// Items fished out of the sewer using chewing gum on a string
		CRIMBO05,	// Items formerly creatable in Crimbo Town during Crimbo 2005
		CRIMBO06,	// Items formerly creatable in Crimbo Town during Crimbo 2006
		CRIMBO07,	// Items formerly creatable in Crimbo Town during Crimbo 2007
		CRIMBO12,	// Items creatable in Crimbo Town during Crimbo 2012
		CRIMBO16,	// Items creatable in Crimbo Town during Crimbo 2016
		SUGAR_FOLDING,	// Items anybody can create by folding a sugar sheet
		PHINEAS,	// Items requiring access to Phineas
		COOK_FANCY,	// Items created with an Dramatic Range
		MIX_FANCY,	// Items created with a Cocktailcrafting Kit
		ACOMBINE,	// Un-untinkerable Ideas
		COINMASTER,	// Coinmaster purchase
		CLIPART,	// Tome of Clip Art summons
		JARLS,		// Items that can only be made by an Avatar of Jarlsberg
		GRANDMA,	// Items made by Grandma Sea Monkee
		CHEMCLASS,	// Items made in Chemistry Class at KOLHS
		ARTCLASS,	// Items made in Art Class at KOLHS
		SHOPCLASS,	// Items made in Shop Class at KOLHS
		BEER,		// Items made with ingredients from A Beer Garden
		JUNK,		// Items made from using a Worse Homes and Gardens
		WINTER,		// Items made with ingredients from A Winter Garden
		RUMPLE,		// Items made at Rumpelstiltskin's Workshop
		FIVE_D,		// Xiblaxian 5d Printer
		VYKEA,		// Items made with VYKEA instructions
		DUTYFREE,	// Elemental International Airport Duty Free Shop
		FLOUNDRY,	// Items made in Clan Floundry
		TERMINAL,	// Items extruded from Source Terminal
		BARREL,		// Items granted at the Barrel Shrine
		WAX,		// Items made from globs of melted wax
		SPANT,		// Items made from spant chitin/tendons
		SPACEGATE,	// Items granted at Spacegate Equipment Requisition
		XO,		// Items made using Xes and Os
		SLIEMCE,		// Items made using slime blobs
		NEWSPAPER,	// Items made from burning newspaper
		METEOROID,	// Items made from metal meteoroid
		FANTASY_REALM,	// Items made by visiting FantasyRealm welcome center
		SAUSAGE_O_MATIC,	// Items made by Kramco Sausage-o-Matic
	}

	public enum CraftingRequirements
	{
		MALE,
		FEMALE,
		SSPD,		// Requires SSPD
		HAMMER,		// Requires tenderizing hammer (implied for SMITH & SSMITH)
		GRIMACITE,	// Requires depleted Grimacite hammer
		TORSO,
		SUPER_MEATSMITHING,
		ARMORCRAFTINESS,
		ELDRITCH,	// Requires Eldritch Intellect
		EXPENSIVE,	// Requires Really Expensive Jewelrycrafting
		REAGENT,	// Requires Advanced Saucecrafting
		WAY,		 // Requires The Way of Sauce
		DEEP_SAUCERY,
		PASTA,
		TRANSNOODLE,	// Requires Transcendental Noodlecraft
		TEMPURAMANCY,	// Requires Tempuramancy
		PATENT,		// Requires Patent Medicine
		AC,		// Requires Advanced Cocktailcrafting
		SHC,		// Requires Superhuman Cocktailcrafting
		SALACIOUS,	// Requires Salacious Cocktailcrafting
		TIKI,		// Requires Tiki Mixology
		NOBEE,		// Not on Bees Hate You path
		BAKE,		// Requires Avatar of Jarlsberg skill Bake
		BLEND,		// Requires Avatar of Jarlsberg skill Blend
		BOIL,		// Requires Avatar of Jarlsberg skill Boil
		CHOP,		// Requires Avatar of Jarlsberg skill Chop
		CURDLE,		// Requires Avatar of Jarlsberg skill Curdle
		FREEZE,		// Requires Avatar of Jarlsberg skill Freeze
		FRY,		// Requires Avatar of Jarlsberg skill Fry
		GRILL,		// Requires Avatar of Jarlsberg skill Grill
		SLICE,		// Requires Avatar of Jarlsberg skill Slice
	}

	public enum CraftingMisc
	{
		TRIPLE_SAUCE,	// Saucerors make 3 of this item at a time
		NODISCOVERY,	// Recipe unexpectedly does not appear in Discoveries
		MANUAL,		// Recipe should never be used automatically
	}

	// Colors which are used to handle the various KoLmafia states.
	// Used when changing the display.

	public static final Color ERROR_COLOR = new Color( 255, 192, 192 );
	public static final Color ENABLED_COLOR = new Color( 192, 255, 192 );
	public static final Color DISABLED_COLOR = null;

	// Constants which are useful, but not necessarily used very often.
	// Includes win game text.

	public static final String DEFAULT_KMAIL = "Keep the contents of this message top-sekrit, ultra hush-hush.";

	public static final String[][] WIN_GAME_TEXT = new String[][]
	{
		{
			"Petitioning the Seaside Town Council for automatic game completion...",
			"The Seaside Town Council has rejected your petition.  Game incomplete.",
			"You reject the Seaside Town's decision.  Fighting the council...",
			"You have been defeated by the Seaside Town Council."
		},

		{
			"You enter the super-secret code into the Strange Leaflet...",
			"Your ruby W and heavy D fuse to form the mysterious R!",
			"Moxie sign backdoor accessed.	Supertinkering The Ultimate Weapon...",
			"Supertinkering complete.  Executing tower script...",
			"Your RNG spawns an enraged cow on Floors 1-6."
		},

		{
			"You win the game. What, you were expecting more?",
			"You are now standing in an open field to the west of the Kingdom.",
			"You hear a gurgling ocean to the south, and a path leads north into Valhalla.",
			"What now, Adventurer?"
		},

		{
			"You touch your star starfish!	You surge with power!",
			"Accessing tower backdoor.  Fighting Naughty Sorceress...",
			"Connection timed out during post.  Retrying...",
			"Connection timed out during reply.  Retrying...",
			"Star power expired.  You slink away, dejected and defeated."
		},

		{
			"You raise your metallic A to the sky. Victory is yours!",
			"Original game concept by Jick (Asymmetric Publications).",
			"Co-written by Mr. Skullhead, Riff, and the /dev team.",
			"Special thanks to: the Mods, the Ascension testers, and you.",
			"We present you a new quest, which is the same thing, only harder.",
			"Segmentation fault.  Core dumped."
		},

		{
			"Unlocking secret Arizona trail script...",
			"Crossing first obstacle, admiring landmarks...",
			"Path set to oxygenarian, familiar pace set to grue-ing...",
			"You have died from KoLera."
		},

		{
			"Challenging Naughty Sorceress to a game of chess...",
			"Naughty Sorceress accepts, and invites you to go first.  Calculating move...",
			"Timeout in findBestMove(), making random move: &#9817;f2-f3",
			"Naughty Sorceress moves: &#9823;e7-e5",
			"Timeout in findBestMove(), making random move: &#9817;g2-g4",
			"Naughty Sorceress moves: &#9819;d8-h4",
			"Checkmate."
		},
	};

	// Variables which relate to a given session.  These are made
	// global in order to ensure that any element of KoLmafia can
	// access session-specific information.

	public static final List<String> saveStateNames = LockableListFactory.getSortedInstance( String.class );

	public static final List<AdventureResult> inventory = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> closet = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> storage = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> unlimited = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> freepulls = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> nopulls = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> collection = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> campground = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> chateau = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> falloutShelter = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<AdventureResult> pulverizeQueue = LockableListFactory.getSortedInstance( AdventureResult.class );

	public static final List<UseSkillRequest> usableSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> summoningSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> remedySkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> selfOnlySkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> buffSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> songSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> expressionSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> walkSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> availableSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final IdentityHashMap<UseSkillRequest, Object> availableSkillsMap = new IdentityHashMap<UseSkillRequest, Object>();
	public static final List<UseSkillRequest> availableCombatSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final IdentityHashMap<UseSkillRequest, Object> availableCombatSkillsMap = new IdentityHashMap<UseSkillRequest, Object>();
	public static final List<UseSkillRequest> permedSkills = LockableListFactory.getInstance( UseSkillRequest.class );
	public static final List<UseSkillRequest> combatSkills = LockableListFactory.getInstance( UseSkillRequest.class );

	public static final List<AdventureResult> activeEffects = LockableListFactory.getInstance( AdventureResult.class );
	public static final ArrayList<AdventureResult> recentEffects = new ArrayList<AdventureResult>();

	public static final List<AdventureResult> hermitItems = LockableListFactory.getInstance( AdventureResult.class );
	public static final List<String> restaurantItems = LockableListFactory.getInstance( String.class );
	public static final List<String> microbreweryItems = LockableListFactory.getInstance( String.class );
	public static final List<String> kitchenItems = LockableListFactory.getInstance( String.class );
	public static final List<String> cafeItems = LockableListFactory.getInstance( String.class );

	public static final List<AdventureResult> tally = LockableListFactory.getSortedInstance( AdventureResult.class );
	public static final List<RegisteredEncounter> adventureList = LockableListFactory.getInstance( RegisteredEncounter.class );
	public static final List<RegisteredEncounter> encounterList = LockableListFactory.getSortedInstance( RegisteredEncounter.class );

	// Locations where session information is displayed for the user.
	// Include just the event history buffer and the command line buffer.

	public static final StyledChatBuffer commandBuffer = new StyledChatBuffer( "", "blue", false );

	public static final Comparator<String> ignoreCaseComparator = new Comparator<String>()
	{
		public int compare( String s1, String s2 )
		{
			return s1.compareToIgnoreCase( s2 );
		}
	};
}
