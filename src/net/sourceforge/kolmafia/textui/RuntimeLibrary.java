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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.lang.reflect.Method;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import org.json.JSONException;
import org.json.JSONObject;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.Expression;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.ModifierExpression;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.MonsterExpression;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.VYKEACompanionData;

import net.sourceforge.kolmafia.chat.ChatMessage;
import net.sourceforge.kolmafia.chat.ChatPoller;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.chat.InternalMessage;
import net.sourceforge.kolmafia.chat.WhoMessage;

import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.moods.Mood;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase.Candy;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.FaxBot;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.CraftRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.EveryCard;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.InternalChatRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.MoneyMakingGameRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.SweetSynthesisRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.DadManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.FamiliarManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MoneyMakingGameManager;
import net.sourceforge.kolmafia.session.MonsterManuelManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.NumberologyManager;
import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.svn.SVNManager;
import net.sourceforge.kolmafia.swingui.FaxRequestFrame;
import net.sourceforge.kolmafia.swingui.widget.InterruptableDialog;

import net.sourceforge.kolmafia.textui.Interpreter.CallFrame;

import net.sourceforge.kolmafia.textui.command.ConditionalStatement;
import net.sourceforge.kolmafia.textui.command.SetPreferencesCommand;

import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.AggregateValue;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.CompositeValue;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayServer;

public abstract class RuntimeLibrary
{
	private static final RecordType itemDropRec = new RecordType(
		"{item drop; int rate; string type;}",
		new String[] { "drop", "rate", "type" },
		new Type[] { DataTypes.ITEM_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE } );
	
	private static final RecordType maximizerResults = new RecordType(
		"{string display; string command; float score; effect effect; item item; skill skill;}",
		new String[] { "display", "command", "score", "effect", "item", "skill" },
		new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.FLOAT_TYPE, DataTypes.EFFECT_TYPE, DataTypes.ITEM_TYPE, DataTypes.SKILL_TYPE } );
	
	private static final RecordType svnInfoRec = new RecordType(
		"{string url; int revision; string last_changed_author; int last_changed_rev; string last_changed_date;}",
		new String[] { "url", "revision", "last_changed_author", "last_changed_rev", "last_changed_date"}, 
		new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE} );

	private static final RecordType stackTraceRec = new RecordType(
		"{string file; string name; int line;}",
		new String[] { "file", "name", "line" },
		new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE } );
	
	private static final AggregateType NumberologyType = new AggregateType( DataTypes.INT_TYPE, DataTypes.INT_TYPE );

	private static final AggregateType ItemSetType = new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.INT_TYPE );

	public static final FunctionList functions = new FunctionList();

	// *** Why can't the following go in KoLConstants?
	public static final Set<String> frameNames = new HashSet<String>();
	static
	{
		for ( String[] frame : KoLConstants.FRAME_NAMES )
		{
			RuntimeLibrary.frameNames.add( frame[ 1 ] );
		}
		RuntimeLibrary.frameNames.add( "AnnouncementFrame" );
		RuntimeLibrary.frameNames.add( "CakeArenaFrame" );
		RuntimeLibrary.frameNames.add( "ChatFrame" );
		RuntimeLibrary.frameNames.add( "CouncilFrame" );
		RuntimeLibrary.frameNames.add( "DescriptionFrame" );
		RuntimeLibrary.frameNames.add( "LoginFrame" );
		RuntimeLibrary.frameNames.add( "MonsterDescriptionFrame" );
		RuntimeLibrary.frameNames.add( "ProfileFrame" );
		RuntimeLibrary.frameNames.add( "RequestSynchFrame" );
		RuntimeLibrary.frameNames.add( "ScriptManageFrame" );
		RuntimeLibrary.frameNames.add( "SendMessageFrame" );
		RuntimeLibrary.frameNames.add( "TabbedChatFrame" );
		RuntimeLibrary.frameNames.add( "TrophyFrame" );
	}

	public static FunctionList getFunctions()
	{
		return RuntimeLibrary.functions;
	}

	static
	{
		Type[] params;

		// Basic utility functions which print information
		// or allow for easy testing.

		Type stackTraceRecArray = new AggregateType( stackTraceRec, 0 );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_stack_trace", stackTraceRecArray, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_version", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_revision", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_path", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_path_full", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_path_variables", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "batch_open", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "batch_close", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "enable", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "disable", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "user_confirm", DataTypes.BOOLEAN_TYPE, params ) );
		
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "user_confirm", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "logprint", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "traceprint", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "print", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "print", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "print_html", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "abort", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "abort", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "cli_execute", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "load_html", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "write", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "writeln", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "form_field", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "form_fields", new AggregateType(
			DataTypes.STRING_TYPE, DataTypes.STRING_TYPE ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "visit_url", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "make_url", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "wait", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "waitq", DataTypes.VOID_TYPE, params ) );

		// Type conversion functions which allow conversion
		// of one data format to another.

		params = new Type[] { DataTypes.ANY_TYPE };
		functions.add( new LibraryFunction( "to_json", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_string", DataTypes.STRING_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_string", DataTypes.STRING_TYPE, params ) );
		params = new Type[] { DataTypes.FLOAT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_boolean", DataTypes.BOOLEAN_TYPE, params ) );
		params = new Type[] { DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "to_boolean", DataTypes.BOOLEAN_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_boolean", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.EFFECT_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.CLASS_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.THRALL_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.SERVANT_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.VYKEA_TYPE };
		functions.add( new LibraryFunction( "to_int", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_float", DataTypes.FLOAT_TYPE, params ) );
		params = new Type[] { DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "to_float", DataTypes.FLOAT_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_float", DataTypes.FLOAT_TYPE, params ) );
		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "to_float", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_item", DataTypes.ITEM_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_item", DataTypes.ITEM_TYPE, params ) );
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_item", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_location", DataTypes.LOCATION_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_location", DataTypes.LOCATION_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_class", DataTypes.CLASS_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_class", DataTypes.CLASS_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_stat", DataTypes.STAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );
		params = new Type[] { DataTypes.EFFECT_TYPE };
		functions.add( new LibraryFunction( "to_skill", DataTypes.SKILL_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );
		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "to_effect", DataTypes.EFFECT_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_familiar", DataTypes.FAMILIAR_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_monster", DataTypes.MONSTER_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_monster", DataTypes.MONSTER_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "image_to_monster", DataTypes.MONSTER_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_slot", DataTypes.SLOT_TYPE, params ) );
		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "to_slot", DataTypes.SLOT_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_element", DataTypes.ELEMENT_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_coinmaster", DataTypes.COINMASTER_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_phylum", DataTypes.PHYLUM_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_bounty", DataTypes.BOUNTY_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_thrall", DataTypes.THRALL_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_thrall", DataTypes.THRALL_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_servant", DataTypes.SERVANT_TYPE, params ) );
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "to_servant", DataTypes.SERVANT_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "to_vykea", DataTypes.VYKEA_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "to_plural", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "to_url", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "desc_to_effect", DataTypes.EFFECT_TYPE, params ) );
		// Functions related to daily information which get
		// updated usually once per day.

		params = new Type[] {};
		functions.add( new LibraryFunction( "holiday", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "today_to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "time_to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "now_to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "format_date_time", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "gameday_to_string", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "gameday_to_int", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "gametime_to_int", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "rollover", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "moon_phase", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "moon_light", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "stat_bonus_today", DataTypes.STAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "stat_bonus_tomorrow", DataTypes.STAT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "session_logs", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "session_logs", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		// Major functions related to adventuring and
		// item management.

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "set_location", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "adventure", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.LOCATION_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "adventure", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "adv1", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "get_ccs_action", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "can_still_steal", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "add_item_condition", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "remove_item_condition", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "goal_exists", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_goal", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_goals", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_moods", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "buy", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buy_using_storage", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "buy_using_storage", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE };
		functions.add( new LibraryFunction( "is_accessible", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE };
		functions.add( new LibraryFunction( "inaccessible_reason", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE };
		functions.add( new LibraryFunction( "visit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sell", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "craft", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "create", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "use", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "eat", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "eatsilent", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "drink", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "overdrink", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "drinksilent", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "chew", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "last_item_message", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "empty_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "put_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_shop_using_storage", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_shop_using_storage", DataTypes.BOOLEAN_TYPE, params ) );

 		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "reprice_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "reprice_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "put_display", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "take_closet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_storage", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_display", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "take_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "autosell", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "hermit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "retrieve_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "faxbot", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "can_faxbot", DataTypes.BOOLEAN_TYPE, params ) );

		// Major functions which provide item-related
		// information.

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_inventory", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_closet", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_storage", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_free_pulls", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_shop", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_shop_log", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_stash", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_campground", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_clan_lounge", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_clan_rumpus", DataTypes.STRING_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_chateau", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_dwelling", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "get_related", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_npc_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_coinmaster_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_tradeable", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_giftable", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_displayable", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_discardable", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "autosell_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "mall_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { RuntimeLibrary.ItemSetType };
		functions.add( new LibraryFunction( "mall_prices", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "mall_prices", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "mall_prices", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "npc_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "shop_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buys_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "buy_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sells_item", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.COINMASTER_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sell_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "historical_price", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "historical_age", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "daily_special", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "refresh_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "refresh_stash", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "available_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "item_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "closet_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "equipped_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "creatable_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "creatable_turns", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "creatable_turns", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "creatable_turns", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "get_ingredients", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "storage_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "display_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "shop_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "stash_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "pulls_remaining", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "stills_available", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_mushroom_plot", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "craft_type", DataTypes.STRING_TYPE, params ) );

		// The following functions pertain to providing updated
		// information relating to the player.

		params = new Type[] {};
		functions.add( new LibraryFunction( "refresh_status", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "restore_hp", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "restore_mp", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mood_execute", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_name", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_id", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_hash", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_sign", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_path", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_muscle_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_mysticality_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_moxie_sign", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_bad_moon", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_class", DataTypes.CLASS_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_level", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_hp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_maxhp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_mp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_maxmp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_primestat", DataTypes.STAT_TYPE, params ) );

		params = new Type[] { DataTypes.STAT_TYPE };
		functions.add( new LibraryFunction( "my_basestat", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STAT_TYPE };
		functions.add( new LibraryFunction( "my_buffedstat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_fury", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_soulsauce", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_discomomentum", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_audience", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_absorbs", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_thunder", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_rain", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_lightning", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_mask", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_maxfury", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_meat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_closet_meat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_storage_meat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_session_meat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_adventures", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_session_adv", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_daycount", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_turncount", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_fullness", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "fullness_limit", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_inebriety", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "inebriety_limit", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_spleen_use", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "spleen_limit", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "can_eat", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "can_drink", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "turns_played", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "total_turns_played", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_ascensions", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "can_interact", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_hardcore", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "pvp_attacks_left", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "current_pvp_stances", new AggregateType( DataTypes.INT_TYPE, DataTypes.STRING_TYPE ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_clan_id", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_clan_name", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "limit_mode", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_florist_plants",
			new AggregateType( new AggregateType( DataTypes.STRING_TYPE, 3 ),
			DataTypes.LOCATION_TYPE ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "total_free_rests", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_ignore_zone_warnings", DataTypes.BOOLEAN_TYPE, params ) );

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "have_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "mp_cost", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "adv_cost", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "soulsauce_cost", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "thunder_cost", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "rain_cost", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "lightning_cost", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "fuel_cost", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "turns_per_cast", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE };
		functions.add( new LibraryFunction( "have_effect", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_effects", new AggregateType(
			DataTypes.INT_TYPE, DataTypes.EFFECT_TYPE ), params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "use_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.SKILL_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "use_skill", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "last_skill_message", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_auto_attack", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "set_auto_attack", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "set_auto_attack", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "attack", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "steal", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "runaway", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "use_skill", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "throw_item", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "throw_items", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "run_choice", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "run_choice", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "last_choice", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "last_decision", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "available_choice_options", DataTypes.INT_TO_STRING_TYPE, params ) );

		params = new Type[] { DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "available_choice_options", DataTypes.INT_TO_STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "in_multi_fight", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "choice_follows_fight", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "run_combat", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "run_combat", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "run_turn", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "stun_skill", DataTypes.SKILL_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "reverse_numberology", NumberologyType, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "reverse_numberology", NumberologyType, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "numberology_prize", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRICT_STRING_TYPE };
		functions.add( new LibraryFunction( "every_card_name", DataTypes.STRING_TYPE, params ) );

		// Equipment functions.

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "can_equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SLOT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "equip", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SLOT_TYPE };
		functions.add( new LibraryFunction( "equipped_item", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "have_equipped", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_outfits", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_custom_outfits", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "all_normal_outfits", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "outfit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "have_outfit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "is_wearing_outfit", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "outfit_pieces", new AggregateType( DataTypes.ITEM_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "outfit_tattoo", DataTypes.STRING_TYPE, params ) );

		// Familiar functions.

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_effective_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_enthroned_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "my_poke_fam", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_bjorned_familiar", DataTypes.FAMILIAR_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "have_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "use_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "enthrone_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "bjornify_familiar", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "familiar_equipment", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "familiar_equipped_equipment", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "familiar_weight", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "equip_all_familiars", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "is_familiar_equipment_locked", DataTypes.BOOLEAN_TYPE, params ) );
		
		params = new Type[] {};
		functions.add( new LibraryFunction( "favorite_familiars", new 
			AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.FAMILIAR_TYPE ), params ) );

		params = new Type[] { DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "lock_familiar_equipment", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "weapon_hands", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "item_type", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "weapon_type", DataTypes.STAT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "get_power", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "minstrel_level", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "minstrel_instrument", DataTypes.ITEM_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "minstrel_quest", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_companion", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_thrall", DataTypes.THRALL_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_servant", DataTypes.SERVANT_TYPE, params ) );

		params = new Type[] { DataTypes.SERVANT_TYPE };
		functions.add( new LibraryFunction( "have_servant", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SERVANT_TYPE };
		functions.add( new LibraryFunction( "use_servant", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_vykea_companion", DataTypes.VYKEA_TYPE, params ) );

		// Random other functions related to current in-game
		// state, not directly tied to the character.

		params = new Type[] {};
		functions.add( new LibraryFunction( "council", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "current_mcd", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "change_mcd", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "current_rad_sickness", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_chef", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_bartender", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_shop", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "have_display", DataTypes.BOOLEAN_TYPE, params ) );
		
		params = new Type[] {};
		functions.add( new LibraryFunction( "hippy_stone_broken", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "get_counters", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "stop_counter", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "eudora", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "eudora", DataTypes.BOOLEAN_TYPE, params ) );

		// String parsing functions.

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "is_integer", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "contains_text", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "starts_with", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "ends_with", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "extract_meat", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "extract_items", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "length", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "char_at", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "index_of", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "index_of", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "last_index_of", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "last_index_of", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "substring", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "substring", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_lower_case", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "to_upper_case", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "leetify", DataTypes.STRING_TYPE, params ) );

		// String buffer functions

		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "append", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "insert", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "delete", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "set_length", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.BUFFER_TYPE };
		functions.add( new LibraryFunction( "append_tail", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "append_replacement", DataTypes.BUFFER_TYPE, params ) );

		// Regular expression functions

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "create_matcher", DataTypes.MATCHER_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "find", DataTypes.BOOLEAN_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "start", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "start", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "end", DataTypes.INT_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "end", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "group", DataTypes.STRING_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "group", DataTypes.STRING_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "group_count", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace_first", DataTypes.STRING_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace_all", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.MATCHER_TYPE };
		functions.add( new LibraryFunction( "reset", DataTypes.MATCHER_TYPE, params ) );
		params = new Type[] { DataTypes.MATCHER_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "reset", DataTypes.MATCHER_TYPE, params ) );

		params = new Type[] { DataTypes.BUFFER_TYPE, DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace_string", DataTypes.BUFFER_TYPE, params ) );
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "replace_string", DataTypes.BUFFER_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "split_string", new AggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "split_string", new AggregateType(
			DataTypes.STRING_TYPE, 0 ), params ) );
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "group_string", DataTypes.REGEX_GROUP_TYPE, params ) );

		// Assorted functions
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "expression_eval", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "modifier_eval", DataTypes.FLOAT_TYPE, params ) );
		
		Type maximizerResultsArray = new AggregateType( maximizerResults, 0 );
		
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "maximize", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "maximize", DataTypes.BOOLEAN_TYPE, params ) );
		
		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "maximize", maximizerResultsArray, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "monster_eval", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "is_online", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "slash_count", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_macro", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_clan", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_clan", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_private", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "chat_notify", DataTypes.VOID_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "who_clan", DataTypes.STRING_TO_BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "get_player_id", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "get_player_name", DataTypes.STRING_TYPE, params ) );

		// Quest handling functions.

		params = new Type[] {};
		functions.add( new LibraryFunction( "tavern", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "tavern", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "hedge_maze", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "tower_door", DataTypes.BOOLEAN_TYPE, params ) );

		// Arithmetic utility functions.

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "random", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "round", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "truncate", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "floor", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "ceil", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "square_root", DataTypes.FLOAT_TYPE, params ) );

		// Versions of min and max that return int (if both arguments
		// are int) or float (if at least one arg is a float)
		//
		// The float versions must come first.

		params = new Type[] { DataTypes.FLOAT_TYPE, DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "min", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "min", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.FLOAT_TYPE, DataTypes.FLOAT_TYPE };
		functions.add( new LibraryFunction( "max", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "max", DataTypes.INT_TYPE, params ) );

		// String encoding/decoding functions.

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "url_encode", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "url_decode", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "entity_encode", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "entity_decode", DataTypes.STRING_TYPE, params ) );

		// Functions to manipulate settings

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "get_all_properties", DataTypes.STRING_TO_BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "property_exists", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "property_exists", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "property_has_default", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "property_default_value", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "get_property", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "get_property", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "set_property", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "remove_property", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "remove_property", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "rename_property", DataTypes.BOOLEAN_TYPE, params ) );

		// Functions for aggregates.

		params = new Type[] { DataTypes.AGGREGATE_TYPE };
		functions.add( new LibraryFunction( "count", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.AGGREGATE_TYPE };
		functions.add( new LibraryFunction( "clear", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.AGGREGATE_TYPE };
		functions.add( new LibraryFunction( "file_to_map", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.AGGREGATE_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "file_to_map", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.AGGREGATE_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "map_to_file", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.AGGREGATE_TYPE, DataTypes.STRING_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "map_to_file", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "file_to_array", DataTypes.INT_TO_STRING_TYPE, params ) );

		// Custom combat helper functions.

		params = new Type[] {};
		functions.add( new LibraryFunction( "my_location", DataTypes.LOCATION_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "last_monster", DataTypes.MONSTER_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "get_monsters", new AggregateType(
			DataTypes.MONSTER_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "get_location_monsters", new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE ), params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "appearance_rates", new AggregateType(
			DataTypes.FLOAT_TYPE, DataTypes.MONSTER_TYPE ), params ) );
		
		params = new Type[] { DataTypes.LOCATION_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "appearance_rates", new AggregateType(
			DataTypes.FLOAT_TYPE, DataTypes.MONSTER_TYPE ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "expected_damage", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "expected_damage", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_level_adjustment", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "weight_adjustment", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mana_cost_modifier", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "combat_mana_cost_modifier", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "raw_damage_absorption", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "damage_absorption_percent", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "damage_reduction", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.ELEMENT_TYPE };
		functions.add( new LibraryFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "elemental_resistance", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "combat_rate_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "initiative_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "experience_bonus", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "meat_drop_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "item_drop_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "buffed_hit_stat", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "current_hit_stat", DataTypes.STAT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_element", DataTypes.ELEMENT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_element", DataTypes.ELEMENT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_attack", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_attack", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_defense", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_defense", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_initiative", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_initiative", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_hp", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_hp", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "monster_phylum", DataTypes.PHYLUM_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_phylum", DataTypes.PHYLUM_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "is_banished", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "jump_chance", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "jump_chance", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "jump_chance", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "jump_chance", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE };
		functions.add( new LibraryFunction( "jump_chance", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "jump_chance", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.LOCATION_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "jump_chance", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "item_drops", DataTypes.ITEM_TO_INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "item_drops", DataTypes.ITEM_TO_INT_TYPE, params ) );

		Type itemDropRecArray = new AggregateType( itemDropRec, 0 );

		params = new Type[] {};
		functions.add( new LibraryFunction( "item_drops_array", itemDropRecArray, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "item_drops_array", itemDropRecArray, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "meat_drop", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "meat_drop", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "will_usually_miss", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "will_usually_dodge", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "dad_sea_monkee_weakness", DataTypes.ELEMENT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "flush_monster_manuel_cache", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE };
		functions.add( new LibraryFunction( "monster_manuel_text", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.MONSTER_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "monster_factoids_available", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "all_monsters_with_id", new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE ), params ) );

		// Modifier introspection

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "numeric_modifier", DataTypes.FLOAT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "boolean_modifier", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "boolean_modifier", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "boolean_modifier", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "string_modifier", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "string_modifier", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "effect_modifier", DataTypes.EFFECT_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "effect_modifier", DataTypes.EFFECT_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "class_modifier", DataTypes.CLASS_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "class_modifier", DataTypes.CLASS_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "skill_modifier", DataTypes.SKILL_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "skill_modifier", DataTypes.SKILL_TYPE, params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "stat_modifier", DataTypes.STAT_TYPE, params ) );

		// Quest status inquiries

		params = new Type[] {};
		functions.add( new LibraryFunction( "white_citadel_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "friars_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "black_market_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "hippy_store_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "dispensary_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "guild_store_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "hidden_temple_unlocked", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "knoll_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "canadia_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "gnomads_available", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "florist_available", DataTypes.BOOLEAN_TYPE, params ) );

		// Path Support

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_trendy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "is_trendy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "is_trendy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "is_trendy", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "is_unrestricted", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.SKILL_TYPE };
		functions.add( new LibraryFunction( "is_unrestricted", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.FAMILIAR_TYPE };
		functions.add( new LibraryFunction( "is_unrestricted", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "is_unrestricted", DataTypes.BOOLEAN_TYPE, params ) );

		// MMG support

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_visit", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_search", DataTypes.VOID_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "mmg_make_bet", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_retract_bet", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.BOOLEAN_TYPE };
		functions.add( new LibraryFunction( "mmg_take_bet", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_my_bets", new AggregateType( DataTypes.INT_TYPE, 0 ), params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_offered_bets", new AggregateType( DataTypes.INT_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_bet_owner", DataTypes.STRING_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_bet_owner_id", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_bet_amount", DataTypes.INT_TYPE, params ) );

		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "mmg_wait_event", DataTypes.INT_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_bet_taker", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_bet_taker_id", DataTypes.STRING_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "mmg_bet_winnings", DataTypes.INT_TYPE, params ) );
		
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "svn_exists", DataTypes.BOOLEAN_TYPE, params ) );
		
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "svn_at_head", DataTypes.BOOLEAN_TYPE, params ) );
		
		params = new Type[] { DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "svn_info", svnInfoRec, params ) );

		params = new Type[] { DataTypes.STRING_TYPE, DataTypes.STRING_TYPE };
		functions.add( new LibraryFunction( "xpath", new AggregateType( DataTypes.STRING_TYPE, 0 ), params ) );

		// Sweet Synthesis
		params = new Type[] { DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "candy_for_tier", new AggregateType( DataTypes.ITEM_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.INT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "candy_for_tier", new AggregateType( DataTypes.ITEM_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sweet_synthesis_pairing", new AggregateType( DataTypes.ITEM_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE, DataTypes.ITEM_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "sweet_synthesis_pairing", new AggregateType( DataTypes.ITEM_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sweet_synthesis_result", DataTypes.EFFECT_TYPE, params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE };
		functions.add( new LibraryFunction( "sweet_synthesis_pair", new AggregateType( DataTypes.ITEM_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "sweet_synthesis_pair", new AggregateType( DataTypes.ITEM_TYPE, 0 ), params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE };
		functions.add( new LibraryFunction( "sweet_synthesis", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.EFFECT_TYPE, DataTypes.INT_TYPE };
		functions.add( new LibraryFunction( "sweet_synthesis", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] { DataTypes.ITEM_TYPE, DataTypes.ITEM_TYPE };
		functions.add( new LibraryFunction( "sweet_synthesis", DataTypes.BOOLEAN_TYPE, params ) );

		params = new Type[] {};
		functions.add( new LibraryFunction( "get_fuel", DataTypes.INT_TYPE, params ) );
	}

	public static Method findMethod( final String name, final Class[] args )
		throws NoSuchMethodException
	{
		return RuntimeLibrary.class.getMethod( name, args );
	}

	private static Value continueValue()
	{
		boolean continueValue = Interpreter.getContinueValue();

		Interpreter.forgetPendingState();

		return DataTypes.makeBooleanValue( continueValue );
	}

	// Support for batching of server requests

	private static void batchCommand( Interpreter interpreter, String cmd, String prefix, String params )
	{
		LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched = interpreter.batched;
		if ( batched == null )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( cmd, prefix == null ? params : (prefix + " " + params) );
			return;
		}

		LinkedHashMap<String, StringBuilder> prefixMap = batched.get( cmd );
		if ( prefixMap == null )
		{
			// First instance of this command
			prefixMap = new LinkedHashMap<String, StringBuilder>();
			batched.put( cmd, prefixMap );
		}

		String key = prefix == null ? "" : prefix;
		StringBuilder buf = prefixMap.get( key );
		if ( buf == null )
		{
			buf = new StringBuilder( params );
			prefixMap.put( key, buf );
		}
		else
		{
			buf.append( ", " );
			buf.append( params );
		}
	}

	public static Value get_stack_trace( Interpreter interpreter )
	{
		List<CallFrame> callStack = interpreter.getCallFrames();

		int frameCount = callStack.size();
		AggregateType type = new AggregateType( RuntimeLibrary.stackTraceRec, frameCount );
		ArrayValue value = new ArrayValue( type );

		// Topmost frame is get_stack_trace().
		for ( int i = 0; i < frameCount - 1; ++i )
		{
			CallFrame frame = callStack.get( frameCount - 2 - i );

			RecordValue rec = (RecordValue) value.aref( new Value( i ) );

			rec.aset( 0, new Value( frame.getFileName() ), null );
			rec.aset( 1, new Value( frame.getName() ), null );
			rec.aset( 2, new Value( frame.getLineNumber() ), null );
		}

		return value;
	}

	public static Value get_version( Interpreter interpreter )
	{
		return new Value( KoLConstants.VERSION_NAME );
	}

	public static Value get_revision( Interpreter interpreter )
	{
		return new Value( StaticEntity.getRevision() );
	}

	public static Value get_path( Interpreter interpreter )
	{
		RelayRequest relayRequest = interpreter.getRelayRequest();
		if ( relayRequest == null )
		{
			return DataTypes.STRING_INIT;
		}
		return new Value( relayRequest.getBasePath() );
	}

	public static Value get_path_full( Interpreter interpreter )
	{
		RelayRequest relayRequest = interpreter.getRelayRequest();
		if ( relayRequest == null )
		{
			return DataTypes.STRING_INIT;
		}
		return new Value( relayRequest.getPath() );
	}

	public static Value get_path_variables( Interpreter interpreter )
	{
		RelayRequest relayRequest = interpreter.getRelayRequest();
		if ( relayRequest == null )
		{
			return DataTypes.STRING_INIT;
		}
		String value = relayRequest.getPath();
		int quest = value.indexOf( "?" );
		return quest == -1 ? DataTypes.STRING_INIT : new Value( value.substring( quest ) );
	}

	public static Value batch_open( Interpreter interpreter )
	{
		if ( interpreter.batched == null )
		{
			interpreter.batched = new LinkedHashMap<String, LinkedHashMap<String, StringBuilder>>();
		}
		return DataTypes.VOID_VALUE;
	}

	public static Value batch_close( Interpreter interpreter )
	{
		LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched = interpreter.batched;
		if ( batched != null )
		{
			Iterator i1 = batched.entrySet().iterator();
			while ( i1.hasNext() && KoLmafia.permitsContinue() )
			{
				Map.Entry e1 = (Map.Entry) i1.next();
				String cmd = (String) e1.getKey();
				LinkedHashMap<String, StringBuilder> prefixes = (LinkedHashMap<String, StringBuilder>) e1.getValue();
				Iterator i2 = prefixes.entrySet().iterator();
				while ( i2.hasNext() && KoLmafia.permitsContinue() )
				{
					Map.Entry e2 = (Map.Entry) i2.next();
					String prefix = (String) e2.getKey();
					String params = ((StringBuilder) e2.getValue()).toString();
					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( cmd, prefix.equals( "" ) ? params : (prefix + " " + params) );
				}
			}
			interpreter.batched = null;
		}

		return RuntimeLibrary.continueValue();
	}
	// Basic utility functions which print information
	// or allow for easy testing.

	public static Value enable( Interpreter interpreter, final Value name )
	{
		StaticEntity.enable( name.toString().toLowerCase() );
		return DataTypes.VOID_VALUE;
	}

	public static Value disable( Interpreter interpreter, final Value name )
	{
		StaticEntity.disable( name.toString().toLowerCase() );
		return DataTypes.VOID_VALUE;
	}

	public static Value user_confirm( Interpreter interpreter, final Value message )
	{
		return DataTypes.makeBooleanValue( InputFieldUtilities.confirm( message.toString() ) );
	}

	public static Value user_confirm( Interpreter interpreter, final Value message, final Value timeOut,
		final Value defaultBoolean )
	{
		return InterruptableDialog.confirm( message, timeOut, defaultBoolean );
	}

	private static String cleanString( Value string )
	{
		String parameters = string.toString();

		parameters = StringUtilities.globalStringDelete( parameters, "\n" );
		parameters = StringUtilities.globalStringDelete( parameters, "\r" );
		
		return parameters;
	}

	public static Value logprint( Interpreter interpreter, final Value string )
	{
		String parameters = RuntimeLibrary.cleanString( string );
		RequestLogger.getSessionStream().println( "> " + parameters );
		return DataTypes.VOID_VALUE;
	}

	public static Value traceprint( Interpreter interpreter, final Value string )
	{
		if ( RequestLogger.isTracing() )
		{
			String parameters = RuntimeLibrary.cleanString( string );
			RequestLogger.trace( "trace: " + parameters );
		}

		return DataTypes.VOID_VALUE;
	}

	public static Value print( Interpreter interpreter, final Value string )
	{
		String parameters = RuntimeLibrary.cleanString( string );

		RequestLogger.getSessionStream().println( "> " + parameters );

		parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

		RequestLogger.printLine( parameters );

		return DataTypes.VOID_VALUE;
	}

	public static Value print( Interpreter interpreter, final Value string, final Value color )
	{
		String parameters = RuntimeLibrary.cleanString( string );

		RequestLogger.getSessionStream().println( "> " + parameters );

		parameters = StringUtilities.globalStringReplace( parameters, "<", "&lt;" );

		String colorString = color.toString();

		colorString = StringUtilities.globalStringDelete( colorString, "\"" );
		colorString = StringUtilities.globalStringDelete( colorString, "<" );

		RequestLogger.printLine( "<font color=\"" + colorString + "\">" + parameters + "</font>" );

		return DataTypes.VOID_VALUE;
	}

	public static Value print_html( Interpreter interpreter, final Value string )
	{
		RequestLogger.printLine( string.toString() );
		return DataTypes.VOID_VALUE;
	}

	public static Value abort( Interpreter interpreter )
	{
		RuntimeLibrary.abort( interpreter, "Script aborted." );
		return DataTypes.VOID_VALUE;
	}

	public static Value abort( Interpreter interpreter, final Value string )
	{
		RuntimeLibrary.abort( interpreter, string.toString() );
		return DataTypes.VOID_VALUE;
	}

	private static Value abort( Interpreter interpreter, final String string )
	{
		KoLmafia.updateDisplay( MafiaState.ABORT, string );
		interpreter.setState( Interpreter.STATE_EXIT );
		return DataTypes.VOID_VALUE;
	}

	public static Value cli_execute( Interpreter interpreter, final Value string )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( string.toString(), interpreter );
		return RuntimeLibrary.continueValue();
	}

	public static Value load_html( Interpreter interpreter, final Value string )
	{
		StringBuffer buffer = new StringBuffer();
		Value returnValue = new Value( DataTypes.BUFFER_TYPE, "", buffer );

		String location = string.toString();
		if ( !location.endsWith( ".htm" ) && !location.endsWith( ".html" ) )
		{
			return returnValue;
		}

		byte[] bytes = DataFileCache.getBytes( location );
		buffer.append( new String( bytes ) );
		return returnValue;
	}

	public static Value write( Interpreter interpreter, final Value string )
	{
		RelayRequest relayRequest = interpreter.getRelayRequest();
		if ( relayRequest == null )
		{
			return DataTypes.VOID_VALUE;
		}

		StringBuffer serverReplyBuffer = interpreter.getServerReplyBuffer();
		serverReplyBuffer.append( string.toString() );
		return DataTypes.VOID_VALUE;
	}

	public static Value writeln( Interpreter interpreter, final Value string )
	{
		RelayRequest relayRequest = interpreter.getRelayRequest();
		if ( relayRequest == null )
		{
			return DataTypes.VOID_VALUE;
		}

		StringBuffer serverReplyBuffer = interpreter.getServerReplyBuffer();
		serverReplyBuffer.append( string.toString() );
		serverReplyBuffer.append( KoLConstants.LINE_BREAK );
		return DataTypes.VOID_VALUE;
	}

	public static Value form_field( Interpreter interpreter, final Value key )
	{
		RelayRequest relayRequest = interpreter.getRelayRequest();
		if ( relayRequest == null )
		{
			return DataTypes.STRING_INIT;
		}

		String value = relayRequest.getFormField( key.toString() );
		return value == null ? DataTypes.STRING_INIT : new Value( value );
	}

	public static Value form_fields( Interpreter interpreter )
	{
		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, DataTypes.STRING_TYPE );
		MapValue value = new MapValue( type );

		RelayRequest relayRequest = interpreter.getRelayRequest();
		if ( relayRequest == null )
		{
			return value;
		}
		for ( String field : relayRequest.getFormFields() )
		{
			String[] pieces = field.split( "=", 2 );
			String name = pieces[ 0 ];
			Value keyval = DataTypes.STRING_INIT;
			if ( pieces.length > 1 )
			{
				keyval = new Value( GenericRequest.decodeField( pieces[ 1 ] ) );
			}
			Value keyname = new Value( name );
			while ( value.contains( keyname ) )
			{	// Make a unique name for duplicate fields
				name = name + "_";
				keyname = new Value( name );
			}
			value.aset( keyname, keyval );
		}
		return value;
	}

	public static Value xpath( Interpreter interpreter, final Value html, final Value xpath )
	{
		HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();

		TagNode doc;
		try
		{
			doc = cleaner.clean( html.toString() );
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
			throw interpreter.runtimeException( "something went wrong while cleaning html" );
		}
		Object[] result;
		try
		{
			result = doc.evaluateXPath( xpath.toString() );
		}
		catch ( XPatherException e )
		{
			throw interpreter.runtimeException( "invalid xpath expression" );
		}

		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, result.length );
		ArrayValue value = new ArrayValue( type );

		// convert Tagnode objects to strings consisting of their inner HTML

		SimpleXmlSerializer serializer = new SimpleXmlSerializer( cleaner.getProperties() );
		
		for ( int i = 0; i < result.length; i++ )
		{
			Object ob = result[ i ];

			if ( ob instanceof TagNode )
			{
				TagNode tag = (TagNode) ob;
				try
				{
					result[ i ] = serializer.getXmlAsString( tag );
				}
				catch ( IOException e )
				{
					StaticEntity.printStackTrace( e );
					throw interpreter.runtimeException( "something went wrong while serializing to html" );
				}
			}

			value.aset( new Value( i ), new Value( result[ i ].toString() ) );
		}

		return value;
	}

	public static Value visit_url( Interpreter interpreter )
	{
		RelayRequest relayRequest = interpreter.getRelayRequest();
		if ( relayRequest == null )
		{
			return new Value( DataTypes.BUFFER_TYPE, "", new StringBuffer() );
		}

		while ( true )
		{
			RequestThread.postRequest( relayRequest );
			if ( relayRequest.redirectLocation == null )
			{
				break;
			}
			relayRequest.constructURLString( relayRequest.redirectLocation, false, false );
			if ( KoLmafiaASH.getClientHTML( relayRequest ) )
			{
				break;
			}
		}

		StringBuffer buffer = new StringBuffer();
		if ( relayRequest.responseText != null )
		{
			buffer.append( relayRequest.responseText );
		}
		return new Value( DataTypes.BUFFER_TYPE, "", buffer );
	}

	public static Value visit_url( Interpreter interpreter, final Value string )
	{
		return RuntimeLibrary.visit_url( interpreter, string.toString(), true, false );
	}

	public static Value visit_url( Interpreter interpreter, final Value string, final Value usePostMethod )
	{
		return RuntimeLibrary.visit_url( interpreter, string.toString(), usePostMethod.intValue() == 1, false );
	}

	public static Value visit_url( Interpreter interpreter, final Value string, final Value usePostMethod, final Value encoded )
	{
		return RuntimeLibrary.visit_url( interpreter, string.toString(), usePostMethod.intValue() == 1, encoded.intValue() == 1 );
	}

	private static Value visit_url( Interpreter interpreter, final String location )
	{
		return RuntimeLibrary.visit_url( interpreter, location, true, false );
	}

	private static Value visit_url( Interpreter interpreter, final String location, final boolean usePostMethod, final boolean encoded )
	{
		StringBuffer buffer = new StringBuffer();
		Value returnValue = new Value( DataTypes.BUFFER_TYPE, "", buffer );

		// See if we are inside a relay override
		boolean inRelayOverride = interpreter.getRelayRequest() != null;

		// If so, use a RelayRequest rather than a GenericRequest
		GenericRequest request = inRelayOverride ? new RelayRequest( false ) : new GenericRequest( "" );

		// Build the desired URL
		request.constructURLString( RelayServer.trimPrefix( location ), usePostMethod, encoded );
		if ( GenericRequest.shouldIgnore( request ) )
		{
			return returnValue;
		}

		// If we are not in a relay script, ignore a request to an unstarted fight
		if ( !inRelayOverride &&
		     request.getPath().equals( "fight.php" ) &&
		     FightRequest.getCurrentRound() == 0 )
		{
			return returnValue;
		}

		// Post the request and get the response!  Note that if we are
		// in a relay script, we have to follow all redirection here.
		while ( true )
		{
			RequestThread.postRequest( request );
			if ( !inRelayOverride || request.redirectLocation == null )
			{
				break;
			}
			request.constructURLString( request.redirectLocation, false, false );
			if ( KoLmafiaASH.getClientHTML( (RelayRequest)request ) )
			{
				break;
			}
		}

		if ( request.responseText != null )
		{
			buffer.append( request.responseText );
		}

		return returnValue;
	}

	public static Value make_url( Interpreter interpreter, final Value arg1, final Value arg2, final Value arg3 )
	{
		String location = arg1.toString();
		boolean usePostMethod = arg2.intValue() == 1;
		boolean encoded = arg3.intValue() == 1;
		GenericRequest request = new GenericRequest( "" );
		request.constructURLString( location, usePostMethod, encoded );
		return new Value( request.getURLString() );
	}

	public static Value wait( Interpreter interpreter, final Value delay )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "wait", delay.toString() );
		return DataTypes.VOID_VALUE;
	}

	public static Value waitq( Interpreter interpreter, final Value delay )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "waitq", delay.toString() );
		return DataTypes.VOID_VALUE;
	}

	// Type conversion functions which allow conversion
	// of one data format to another.

	public static Value to_json( Interpreter interpreter, Value val )
		throws JSONException
	{
		Object obj = val.asProxy().toJSON();
		return new Value( obj instanceof String ? JSONObject.quote( (String) obj ) : obj.toString() );
	}

	public static Value to_string( Interpreter interpreter, Value val )
	{
		// This function previously just returned val, except in the
		// case of buffers in which case it's necessary to capture the
		// current string value of the buffer.	That works fine in most
		// cases, but NOT if the value ever gets used as a key in a
		// map; having a key that's actually an int (for example) in a
		// string map causes the map ordering to become inconsistent,
		// because int Values compare differently than string Values.
		return val.toStringValue();
	}
	
	public static Value to_string( Interpreter interpreter, Value val, Value fmt )
	{
		try
		{
			Object arg;
			if ( val.getType().equals( DataTypes.TYPE_FLOAT ) )
			{
				arg = new Double( val.floatValue() );
			}
			else
			{
				arg = new Long( val.intValue() );
			}
			return new Value( String.format( fmt.toString(), arg ) );
		}
		catch ( IllegalFormatException e )
		{
			throw interpreter.runtimeException( "Invalid format pattern" );
		}
	}

	public static Value to_boolean( Interpreter interpreter, final Value value )
	{
		return DataTypes.makeBooleanValue( ( value.intValue() != 0 || value.toString().equalsIgnoreCase( "true" ) ) );
	}

	public static Value to_int( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_STRING ) )
		{
			String string = value.toString();
			try
			{
				return new Value( StringUtilities.parseLongInternal1( string, true ) );
			}
			catch ( NumberFormatException e )
			{
			}

			// Try again with lax parsing

			try
			{
				long retval = StringUtilities.parseLongInternal2( string );
				Exception ex = interpreter.runtimeException( "The string \"" + string + "\" is not an integer; returning " + retval );
				RequestLogger.printLine( ex.getMessage() );
				return new Value( retval );
			}
			catch ( NumberFormatException e )
			{
				// Even with lax parsing, we failed.
				Exception ex = interpreter.runtimeException( "The string \"" + string + "\" does not look like an integer; returning 0" );
				RequestLogger.printLine( ex.getMessage() );
				return DataTypes.ZERO_VALUE;
			}
		}

		return new Value( value.intValue() );
	}

	public static Value to_float( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_STRING ) )
		{
			String string = value.toString();
			try
			{
				return new Value( StringUtilities.parseFloat( string ) );
			}
			catch ( NumberFormatException e )
			{
				Exception ex = interpreter.runtimeException( "The string \"" + string + "\" is not a float; returning 0.0" );
				RequestLogger.printLine( ex.getMessage() );
				return DataTypes.ZERO_FLOAT_VALUE;
			}
		}

		return value.toFloatValue();
	}

	public static Value to_item( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeItemValue( (int) value.intValue(), true );
		}

		String s1 = value.toString();
		Value item = DataTypes.parseItemValue( s1, true, true );
		DataTypes.ITEM_TYPE.validateValue( interpreter, s1, item );

		return item;
	}

	public static Value to_item( Interpreter interpreter, final Value name, final Value count )
	{
		return DataTypes.makeItemValue( ItemDatabase.getItemId( name.toString(), (int) count.intValue() ), true );
	}

	public static Value to_class( Interpreter interpreter, final Value value )
	{
		String name = null;

		if ( value.getType() == DataTypes.INT_TYPE )
		{
			int num = (int) value.intValue();

			if ( num >= 0 && num < DataTypes.CLASSES.length )
			{
				name = DataTypes.CLASSES[ num ];
			}
		}
		else
		{
			name = value.toString();
		}

		return DataTypes.parseClassValue( name, true );
	}

	public static Value to_stat( Interpreter interpreter, final Value value )
	{
		return DataTypes.parseStatValue( value.toString(), true );
	}

	public static Value to_skill( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeSkillValue( (int) value.intValue(), true );
		}

		if ( value.getType().equals( DataTypes.TYPE_EFFECT ) )
		{
			return DataTypes.parseSkillValue( UneffectRequest.effectToSkill( value.toString() ), true );
		}

		return DataTypes.parseSkillValue( value.toString(), true );
	}

	public static Value desc_to_effect( Interpreter interpreter, final Value value )
	{
		return DataTypes.makeEffectValue( EffectDatabase.getEffectIdFromDescription( value.toString() ), true );
	}


	public static Value to_effect( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeEffectValue( (int) value.intValue(), true );
		}

		if ( value.getType().equals( DataTypes.TYPE_SKILL ) )
		{
			return DataTypes.parseEffectValue( UneffectRequest.skillToEffect( value.toString() ), true );
		}

		String s1 = value.toString();
		Value effect = DataTypes.parseEffectValue( s1, true );
		DataTypes.EFFECT_TYPE.validateValue( interpreter, s1, effect );

		return effect;
	}

	public static Value to_location( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.parseLocationValue( (int) value.intValue(), true );
		}
		else
		{
			return DataTypes.parseLocationValue( value.toString(), true );
		}
	}

	public static Value to_familiar( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeFamiliarValue( (int) value.intValue(), true );
		}

		return DataTypes.parseFamiliarValue( value.toString(), true );
	}

	public static Value to_monster( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeMonsterValue( (int) value.intValue(), true );
		}

		return DataTypes.parseMonsterValue( value.toString(), true );
	}

	public static Value image_to_monster( Interpreter interpreter, final Value value )
	{
		MonsterData monster = MonsterDatabase.findMonsterByImage( value.toString() );
		return DataTypes.makeMonsterValue( monster );
	}

	public static Value to_slot( Interpreter interpreter, final Value item )
	{
		if ( !item.getType().equals( DataTypes.TYPE_ITEM ) )
		{
			return DataTypes.parseSlotValue( item.toString(), true );
		}
		switch ( ItemDatabase.getConsumptionType( (int) item.intValue() ) )
		{
		case KoLConstants.EQUIP_HAT:
			return DataTypes.parseSlotValue( "hat", true );
		case KoLConstants.EQUIP_WEAPON:
			return DataTypes.parseSlotValue( "weapon", true );
		case KoLConstants.EQUIP_OFFHAND:
			return DataTypes.parseSlotValue( "off-hand", true );
		case KoLConstants.EQUIP_SHIRT:
			return DataTypes.parseSlotValue( "shirt", true );
		case KoLConstants.EQUIP_PANTS:
			return DataTypes.parseSlotValue( "pants", true );
		case KoLConstants.EQUIP_CONTAINER:
			return DataTypes.parseSlotValue( "container", true );
		case KoLConstants.EQUIP_FAMILIAR:
			return DataTypes.parseSlotValue( "familiar", true );
		case KoLConstants.EQUIP_ACCESSORY:
			return DataTypes.parseSlotValue( "acc1", true );
		default:
			return DataTypes.parseSlotValue( "none", true );
		}
	}

	public static Value to_element( Interpreter interpreter, final Value value )
	{
		return DataTypes.parseElementValue( value.toString(), true );
	}

	public static Value to_coinmaster( Interpreter interpreter, final Value value )
	{
		return DataTypes.parseCoinmasterValue( value.toString(), true );
	}

	public static Value to_phylum( Interpreter interpreter, final Value value )
	{
		return DataTypes.parsePhylumValue( value.toString(), true );
	}

	public static Value to_bounty( Interpreter interpreter, final Value value )
	{
		String stringValue = value.toString();
		int numberIndex = stringValue.indexOf( ":" );
		return numberIndex != -1 ? DataTypes.parseBountyValue( stringValue.substring( 0, numberIndex ), true ) :
			DataTypes.parseBountyValue( stringValue, true );
	}

	public static Value to_thrall( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeThrallValue( (int) value.intValue(), true );
		}

		return DataTypes.parseThrallValue( value.toString(), true );
	}

	public static Value to_servant( Interpreter interpreter, final Value value )
	{
		if ( value.getType().equals( DataTypes.TYPE_INT ) )
		{
			return DataTypes.makeServantValue( (int) value.intValue(), true );
		}

		return DataTypes.parseServantValue( value.toString(), true );
	}

	public static Value to_vykea( Interpreter interpreter, final Value value )
	{
		return DataTypes.parseVykeaValue( value.toString(), true );
	}

	public static Value to_plural( Interpreter interpreter, final Value item ) {
		return new Value( ItemDatabase.getPluralName( (int) item.intValue() ) );
	}

	public static Value to_url( Interpreter interpreter, final Value value )
	{
		KoLAdventure adventure = (KoLAdventure) value.rawValue();
		return ( adventure == null ) ? DataTypes.STRING_INIT : new Value( adventure.getRequest().getURLString() );
	}

	// Functions related to daily information which get
	// updated usually once per day.

	public static Value holiday( Interpreter interpreter )
	{
		Date today = new Date();
		String gameHoliday = HolidayDatabase.getGameHoliday( today );
		String realHoliday = HolidayDatabase.getRealLifeHoliday( today );
		String result =
			gameHoliday != null && realHoliday != null ?
			gameHoliday + "/" + realHoliday :
			gameHoliday != null ?
			gameHoliday :
			realHoliday != null ?
			realHoliday :
			"";
		if ( result.equals( "St. Sneaky Pete's Day/Feast of Boris" ) ||
		     result.equals( "Feast of Boris/St. Sneaky Pete's Day" ) )
		{
			result = "Drunksgiving";
		}
		return new Value( result );
	}

	public static Value today_to_string( Interpreter interpreter )
	{
		return new Value( KoLConstants.DAILY_FORMAT.format( new Date() ) );
	}

	public static Value time_to_string( Interpreter interpreter )
	{
		Calendar timestamp = new GregorianCalendar();
		return new Value( KoLConstants.TIME_FORMAT.format( timestamp.getTime() ) );
	}

	public static Value now_to_string( Interpreter interpreter, Value dateFormatValue )
	{
		Calendar timestamp = new GregorianCalendar();
		SimpleDateFormat dateFormat = new SimpleDateFormat( dateFormatValue.toString() );
		return new Value( dateFormat.format( timestamp.getTime() ) );
	}

	public static Value format_date_time( Interpreter interpreter, Value inFormat, Value dateTimeString, Value outFormat )
	{
		Date inDate = null;
		SimpleDateFormat dateFormat = null;
		Value retVal = null;

		try
		{
			dateFormat = new SimpleDateFormat( inFormat.toString() );
			inDate = dateFormat.parse( dateTimeString.toString() );
			dateFormat= new SimpleDateFormat( outFormat.toString() );
			retVal = new Value( dateFormat.format( inDate ) );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			retVal = new Value( "Bad parameter(s) passed to format_date_time" );
		}
		return retVal;
	}

	public static Value gameday_to_string( Interpreter interpreter )
	{
		return new Value( HolidayDatabase.getCalendarDayAsString( HolidayDatabase.getCalendarDay( new Date() ) ) );
	}

	public static Value gameday_to_int( Interpreter interpreter )
	{
		return new Value( HolidayDatabase.getCalendarDay( new Date() ) );
	}

	public static Value gametime_to_int( Interpreter interpreter )
	{
		return new Value( HolidayDatabase.getTimeDifference( new Date() ) );
	}

	public static Value rollover( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getRollover() );
	}

	public static Value moon_phase( Interpreter interpreter )
	{
		return new Value( HolidayDatabase.getPhaseStep() );
	}

	public static Value moon_light( Interpreter interpreter )
	{
		return new Value( HolidayDatabase.getMoonlight() );
	}

	public static Value stat_bonus_today( Interpreter interpreter )
	{
		if ( ConditionalStatement.test( "today is muscle day" ) )
		{
			return DataTypes.MUSCLE_VALUE;
		}

		if ( ConditionalStatement.test( "today is myst day" ) )
		{
			return DataTypes.MYSTICALITY_VALUE;
		}

		if ( ConditionalStatement.test( "today is moxie day" ) )
		{
			return DataTypes.MOXIE_VALUE;
		}

		return DataTypes.STAT_INIT;
	}

	public static Value stat_bonus_tomorrow( Interpreter interpreter )
	{
		if ( ConditionalStatement.test( "tomorrow is muscle day" ) )
		{
			return DataTypes.MUSCLE_VALUE;
		}

		if ( ConditionalStatement.test( "tomorrow is myst day" ) )
		{
			return DataTypes.MYSTICALITY_VALUE;
		}

		if ( ConditionalStatement.test( "tomorrow is moxie day" ) )
		{
			return DataTypes.MOXIE_VALUE;
		}

		return DataTypes.STAT_INIT;
	}

	public static Value session_logs( Interpreter interpreter, final Value dayCount )
	{
		return RuntimeLibrary.getSessionLogs( interpreter, KoLCharacter.getUserName(), (int) dayCount.intValue() );
	}

	public static Value session_logs( Interpreter interpreter, final Value player, final Value dayCount )
	{
		return RuntimeLibrary.getSessionLogs( interpreter, player.toString(), (int) dayCount.intValue() );
	}

	private static Value getSessionLogs( Interpreter interpreter, final String name, final int dayCount )
	{
		if ( dayCount < 0 )
		{
			throw interpreter.runtimeException( "Can't get session logs for a negative number of days" );
		}

		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, dayCount );
		ArrayValue value = new ArrayValue( type );

		if ( dayCount < 1 )
		{
			return value;
		}

		Calendar timestamp = Calendar.getInstance( TimeZone.getTimeZone("GMT-0330") );
		StringBuilder contents = new StringBuilder();

		for ( int i = 0; i < dayCount; ++i )
		{
			String filename =
				StringUtilities.globalStringReplace( name, " ", "_" ) + "_" + KoLConstants.DAILY_FORMAT.format( timestamp.getTime() ) + ".txt";

			File path = new File( KoLConstants.SESSIONS_LOCATION, filename );
			BufferedReader reader = null;
			if ( !path.exists() )
			{
				filename = filename + ".gz";
				File gzpath = new File( KoLConstants.SESSIONS_LOCATION, filename );
				if ( gzpath.exists() )
				{
					try
					{
						reader = DataUtilities.getReader( new GZIPInputStream( DataUtilities.getInputStream( gzpath ) ) );
					}
					catch ( IOException e )
					{
						StaticEntity.printStackTrace( e );
						reader = null;
					}
				}
			}
			else
			{
				reader = FileUtilities.getReader( path );
			}

			timestamp.add( Calendar.DATE, -1 );

			if ( reader == null )
			{
				continue;
			}

			try
			{
				contents.setLength( 0 );
				String line;

				while ( ( line = reader.readLine() ) != null )
				{
					contents.append( line );
					contents.append( KoLConstants.LINE_BREAK );
				}
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}

			value.aset( new Value( i ), new Value( contents.toString() ) );
		}

		return value;
	}

	// Major functions related to adventuring and
	// item management.

	public static Value adventure( Interpreter interpreter, final Value countValue, final Value locationValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "adventure", count + " " + locationValue );
		return RuntimeLibrary.continueValue();
	}

	public static Value adventure( Interpreter interpreter, final Value countValue, final Value locationValue, final Value filterFunction )
	{
		try
		{
			String filter = filterFunction.toString();
			Macrofier.setMacroOverride( filter, interpreter );

			return RuntimeLibrary.adventure( interpreter, countValue, locationValue );
		}
		finally
		{
			Macrofier.resetMacroOverride();
		}
	}

	public static Value adv1( Interpreter interpreter, final Value locationValue, final Value adventuresUsedValue, final Value filterFunction )
	{
		KoLAdventure adventure = (KoLAdventure) locationValue.rawValue();

		if ( adventure == null )
		{
			return RuntimeLibrary.continueValue();
		}

		boolean redoSkippedAdventures = KoLmafia.redoSkippedAdventures;
		try
		{
			adventure.overrideAdventuresUsed( (int) adventuresUsedValue.intValue() );
			Macrofier.setMacroOverride( filterFunction.toString(), interpreter );
			KoLmafia.redoSkippedAdventures = false;

			KoLmafia.makeRequest( adventure, 1 );
		}
		finally
		{
			KoLmafia.redoSkippedAdventures = redoSkippedAdventures;
			Macrofier.resetMacroOverride();
			adventure.overrideAdventuresUsed( -1 );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value get_ccs_action( Interpreter interpreter, final Value index )
	{
		return new Value(
			CombatActionManager.getCombatAction(
				FightRequest.getCurrentKey(), (int) index.intValue(), true ) );
	}

	public static Value can_still_steal( Interpreter interpreter )
	{
		return new Value( FightRequest.canStillSteal() );
	}

	public static Value add_item_condition( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		int itemId = (int) item.intValue();
		if ( count <= 0 || itemId <= 0 )
		{
			return DataTypes.VOID_VALUE;
		}

		GoalManager.addItemGoal( itemId, count );
		return DataTypes.VOID_VALUE;
	}

	public static Value remove_item_condition( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		int itemId = (int) item.intValue();
		if ( count <= 0 || itemId <= 0 )
		{
			return DataTypes.VOID_VALUE;
		}

		GoalManager.addItemGoal( itemId, 0 - count );
		return DataTypes.VOID_VALUE;
	}

	public static Value goal_exists( Interpreter interpreter, final Value check )
	{
		String checkType = check.toString();

		for ( AdventureResult goal : GoalManager.getGoals() )
		{
			if ( checkType.equals( goal.getConditionType() ) )
			{
				return DataTypes.TRUE_VALUE;
			}
		}

		return DataTypes.FALSE_VALUE;
	}

	public static Value is_goal( Interpreter interpreter, final Value item )
	{
		return DataTypes.makeBooleanValue( GoalManager.hasItemGoal( (int) item.intValue() ) );
	}

	public static Value get_goals( Interpreter interpreter )
	{
		List<AdventureResult> goals = GoalManager.getGoals();
		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, goals.size() );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < goals.size(); ++i )
		{
			value.aset( new Value( i ), new Value( goals.get(i).toConditionString() ) );
		}

		return value;
	}

	public static Value get_moods( Interpreter interpreter )
	{
		List<Mood> moods = MoodManager.getAvailableMoods();
		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, moods.size() );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < moods.size(); ++i )
		{
			value.aset( new Value( i ), new Value( moods.get( i ).getName() ) );
		}

		return value;
	}

	public static Value buy( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) item.intValue();
		AdventureResult itemToBuy = ItemPool.get( itemId );
		int initialAmount = itemToBuy.getCount( KoLConstants.inventory );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "buy", count + " \u00B6" + itemId );
		return DataTypes.makeBooleanValue( initialAmount + count == itemToBuy.getCount( KoLConstants.inventory ) );
	}

	public static Value buy( Interpreter interpreter, final Value arg1, final Value arg2, final Value arg3 )
	{
		if ( arg1.getType().equals( DataTypes.TYPE_COINMASTER ) )
		{
			return RuntimeLibrary.coinmaster_buy( interpreter, arg1, arg2, arg3 );
		}

		int count = (int) arg1.intValue();
		if ( count <= 0 )
		{
			return DataTypes.ZERO_VALUE;
		}

		int itemId = (int) arg2.intValue();
		AdventureResult itemToBuy = ItemPool.get( itemId );
		int initialAmount = itemToBuy.getCount( KoLConstants.inventory );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "buy", count + " \u00B6" + itemId + "@" + arg3.intValue() );
		return new Value( itemToBuy.getCount( KoLConstants.inventory ) - initialAmount );
	}

	public static Value buy_using_storage( Interpreter interpreter, final Value countValue, final Value item )
	{
		if ( KoLCharacter.canInteract() )
		{
			return DataTypes.FALSE_VALUE;
		}

		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return DataTypes.TRUE_VALUE;
		}

		int itemId = (int) item.intValue();
		AdventureResult itemToBuy = ItemPool.get( itemId );
		int initialAmount = itemToBuy.getCount( KoLConstants.storage );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "buy", "using storage " + count + " \u00B6" + itemId );
		return DataTypes.makeBooleanValue( initialAmount + count == itemToBuy.getCount( KoLConstants.storage ) );
	}

	public static Value buy_using_storage( Interpreter interpreter, final Value countValue, final Value item, final Value limitValue )
	{
		if ( KoLCharacter.canInteract() )
		{
			return DataTypes.ZERO_VALUE;
		}

		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return DataTypes.ZERO_VALUE;
		}

		int itemId = (int) item.intValue();
		AdventureResult itemToBuy = ItemPool.get( itemId );
		int initialAmount = itemToBuy.getCount( KoLConstants.storage );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "buy", "using storage " + count + " \u00B6" + itemId + "@" + limitValue.intValue() );
		return new Value( itemToBuy.getCount( KoLConstants.storage ) - initialAmount );
	}

	// Coinmaster functions

	public static Value is_accessible( Interpreter interpreter, final Value master )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		return DataTypes.makeBooleanValue( data != null && data.isAccessible() );
	}

	public static Value inaccessible_reason( Interpreter interpreter, final Value master )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		String reason = data != null ? data.accessible() : null;
		return new Value( reason != null ? reason : "" );
	}

	public static Value visit( Interpreter interpreter, final Value master )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		CoinMasterRequest.visit( data );
		return RuntimeLibrary.continueValue();
	}

	private static Value coinmaster_buy( Interpreter interpreter, final Value master, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}
		CoinmasterData data = (CoinmasterData) master.rawValue();
		AdventureResult item = ItemPool.get( (int) itemValue.intValue(), count );
		int initialAmount = item.getCount( KoLConstants.inventory );
		CoinMasterRequest.buy( data, item );
		return DataTypes.makeBooleanValue( initialAmount + count == item.getCount( KoLConstants.inventory ) );
	}

	public static Value sell( Interpreter interpreter, final Value master, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		CoinmasterData data = (CoinmasterData) master.rawValue();
		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "coinmaster";
			String prefix = "sell " + data.getNickname();
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			AdventureResult item = ItemPool.get( itemId, count );
			CoinMasterRequest.sell( data, item );
		}
		return RuntimeLibrary.continueValue();
	}

	public static Value craft( Interpreter interpreter, final Value modeValue, final Value countValue, final Value item1, final Value item2 )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return DataTypes.ZERO_VALUE;
		}

		String mode = modeValue.toString();
		int id1 = (int) item1.intValue();
		int id2 = (int) item2.intValue();

		CraftRequest req = new CraftRequest( mode, count, id1, id2 );
		RequestThread.postRequest( req );
		return new Value( req.created() );
	}

	public static Value create( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "create", count + " \u00B6" + (int) item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value use( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "use", count + " \u00B6" + (int) item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value eat( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "eat", count + " \u00B6" + (int) item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value eatsilent( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "eatsilent", count + " \u00B6" + (int) item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value drink( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "drink", count + " \u00B6" + (int) item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value overdrink( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "overdrink", count + " \u00B6" + (int) item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value drinksilent( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "drinksilent", count + " \u00B6" + (int) item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value chew( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "chew", count + " \u00B6" + (int) item.intValue() );
		return UseItemRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value last_item_message( Interpreter interpreter )
	{
		return new Value( UseItemRequest.lastUpdate );
	}

	public static Value empty_closet( Interpreter interpreter )
	{
		if ( interpreter.batched != null )
		{
			RuntimeLibrary.batchCommand( interpreter, "closet", null, "empty" );
		}
		else
		{
			ClosetRequest request = new ClosetRequest( ClosetRequest.EMPTY_CLOSET );
			RequestThread.postRequest( request );
		}
		return RuntimeLibrary.continueValue();
	}

	public static Value put_closet( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "closet";
			String prefix = "put";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			ClosetRequest request = new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, ItemPool.get( itemId, count ) );
			RequestThread.postRequest( request );
		}
		return RuntimeLibrary.continueValue();
	}

	public static Value put_closet( Interpreter interpreter, final Value meatValue )
	{
		long meat = meatValue.intValue();
		if ( meat <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		if ( interpreter.batched != null )
		{
			String cmd = "closet";
			String prefix = "put";
			String params = meat + " meat";
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			ClosetRequest request = new ClosetRequest( ClosetRequest.MEAT_TO_CLOSET, (int) meat );
			RequestThread.postRequest( request );
		}
		return RuntimeLibrary.continueValue();
	}

	public static Value put_shop( Interpreter interpreter, final Value priceValue, final Value limitValue, final Value itemValue )
	{
			return put_shop( interpreter, priceValue, limitValue, InventoryManager.getCount( (int) itemValue.contentLong ), itemValue, false );
	}

	public static Value put_shop( Interpreter interpreter, final Value priceValue, final Value limitValue, final Value qtyValue, final Value itemValue )
	{
		return put_shop( interpreter, priceValue, limitValue, qtyValue.contentLong, itemValue, false );
	}

	public static Value put_shop_using_storage( Interpreter interpreter, final Value priceValue, final Value limitValue, final Value itemValue )
	{
		AdventureResult item = ItemPool.get( (int) itemValue.intValue(), 0 );
		return put_shop( interpreter, priceValue, limitValue, item.getCount( KoLConstants.storage ), itemValue, true );
	}

	public static Value put_shop_using_storage( Interpreter interpreter, final Value priceValue, final Value limitValue, final Value qtyValue, final Value itemValue )
	{
		return put_shop( interpreter, priceValue, limitValue, qtyValue.contentLong, itemValue, true );
	}

	// Used internally only.
	private static Value put_shop( Interpreter interpreter, final Value priceValue, final Value limitValue, final long qty, final Value itemValue, boolean usingStorage )
	{
		if ( qty <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();
		int price = (int) priceValue.intValue();
		int limit = (int) limitValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "shop";
			String prefix = usingStorage ? "put using storage" : "put";
			String params = qty + " \u00B6" + itemId + " @ " + price + " limit " + limit;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			AdventureResult [] items = { ItemPool.get( itemId, (int) qty ) };
			int [] prices = { price };
			int [] limits = { limit };

			ManageStoreRequest request = new ManageStoreRequest( items, prices, limits, usingStorage );
			RequestThread.postRequest( request );
		}
		return RuntimeLibrary.continueValue();
	}

	public static Value reprice_shop( Interpreter interpreter, final Value priceValue, final Value itemValue )
	{
		int itemId = (int) itemValue.intValue();
		return reprice_shop( interpreter, priceValue, new Value( StoreManager.getLimit( itemId ) ), itemValue );
	}

	public static Value reprice_shop( Interpreter interpreter, final Value priceValue, final Value limitValue, final Value itemValue )
	{
		int itemId = (int) itemValue.intValue();
		int price = (int) priceValue.intValue();
		int limit = (int) limitValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "shop";
			String prefix = "reprice";
			String params = "\u00B6" + itemId + " @ " + price + " limit " + limit;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			int [] itemIds = { itemId };
			int [] prices = { price };
			int [] limits = { limit };

			ManageStoreRequest request = new ManageStoreRequest( itemIds, prices, limits );
			RequestThread.postRequest( request );
		}
		return RuntimeLibrary.continueValue();
	}

	public static Value put_stash( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "stash";
			String prefix = "put";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			AdventureResult[] items = new AdventureResult[ 1 ];
			items[ 0 ] = ItemPool.get( itemId, count );
			ClanStashRequest request = new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH );
			RequestThread.postRequest( request );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value put_display( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "display";
			String prefix = "put";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			AdventureResult[] items = new AdventureResult[ 1 ];
			items[ 0 ] = ItemPool.get( itemId, count );
			DisplayCaseRequest request = new DisplayCaseRequest( items, true );
			RequestThread.postRequest( request );
		}
		return RuntimeLibrary.continueValue();
	}

	public static Value take_closet( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "closet";
			String prefix = "take";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			ClosetRequest request = new ClosetRequest( ClosetRequest.CLOSET_TO_INVENTORY, ItemPool.get( itemId, count ) );
			RequestThread.postRequest( request );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value take_closet( Interpreter interpreter, final Value meatValue )
	{
		long meat = meatValue.intValue();
		if ( meat <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		if ( interpreter.batched != null )
		{
			String cmd = "closet";
			String prefix = "take";
			String params = meat + " meat";
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			ClosetRequest request = new ClosetRequest( ClosetRequest.MEAT_TO_INVENTORY, (int) meat );
			RequestThread.postRequest( request );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value take_shop( Interpreter interpreter, final Value itemValue )
	{
		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "shop";
			String prefix = "take";
			String params = "all \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			List list = StoreManager.getSoldItemList();
			SoldItem soldItem = new SoldItem( itemId, 0, 0, 0, 0 );
			int index = list.indexOf( soldItem );

			if ( index < 0 )
			{
				return RuntimeLibrary.continueValue();
			}

			soldItem = (SoldItem) list.get( index );

			int count = soldItem.getQuantity();

			ManageStoreRequest request = new ManageStoreRequest( itemId, count );
			RequestThread.postRequest( request );
		}
		return RuntimeLibrary.continueValue();
	}

	public static Value take_shop( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "shop";
			String prefix = "take";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			ManageStoreRequest request = new ManageStoreRequest( itemId, count );
			RequestThread.postRequest( request );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value take_storage( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "hagnk";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, null, params );
		}
		else
		{
			StorageRequest request = new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, ItemPool.get( itemId, count ) );
			RequestThread.postRequest( request );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value take_display( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "display";
			String prefix = "take";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			AdventureResult[] items = new AdventureResult[ 1 ];
			items[ 0 ] = ItemPool.get( itemId, count );
			DisplayCaseRequest request = new DisplayCaseRequest( items, false );
			RequestThread.postRequest( request );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value take_stash( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "stash";
			String prefix = "take";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, prefix, params );
		}
		else
		{
			AdventureResult[] items = new AdventureResult[ 1 ];
			items[ 0 ] = ItemPool.get( itemId, count );
			ClanStashRequest request = new ClanStashRequest( items, ClanStashRequest.STASH_TO_ITEMS );
			RequestThread.postRequest( request );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value autosell( Interpreter interpreter, final Value countValue, final Value itemValue )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		int itemId = (int) itemValue.intValue();

		if ( interpreter.batched != null )
		{
			String cmd = "sell";
			String params = count + " \u00B6" + itemId;
			RuntimeLibrary.batchCommand( interpreter, cmd, null, params );
		}
		else
		{
			AdventureResult[] items = new AdventureResult[ 1 ];
			items[ 0 ] = ItemPool.get( itemId, count );
			AutoSellRequest request = new AutoSellRequest( items );
			RequestThread.postRequest( request );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value hermit( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "hermit", count + " " + ItemDatabase.getItemName( (int) item.intValue() ) );
		return RuntimeLibrary.continueValue();
	}

	public static Value retrieve_item( Interpreter interpreter, final Value countValue, final Value item )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		return DataTypes.makeBooleanValue( InventoryManager.retrieveItem( ItemPool.get( (int) item.intValue(), count ) ) );
	}

	public static Value faxbot( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		FaxBotDatabase.configure();

		String actualName = monster.getName();
		for ( FaxBot bot : FaxBotDatabase.faxbots )
		{
			if ( bot == null )
			{
				continue;
			}

			String botName = bot.getName();
			if ( botName == null )
			{
				continue;
			}

			Monster monsterObject = bot.getMonsterByActualName( actualName );
			if ( monsterObject == null )
			{
				continue;
			}

			if ( !FaxRequestFrame.isBotOnline( botName ) )
			{
				continue;
			}
			return DataTypes.makeBooleanValue( FaxRequestFrame.requestFax( botName, monsterObject, false ) );
		}
		return DataTypes.FALSE_VALUE;
	}

	public static Value can_faxbot( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		FaxBotDatabase.configure();

		String actualName = monster.getName();
		for ( FaxBot bot : FaxBotDatabase.faxbots )
		{
			if ( bot == null )
			{
				continue;
			}

			String botName = bot.getName();
			if ( botName == null )
			{
				continue;
			}

			Monster monsterObject = bot.getMonsterByActualName( actualName );
			if ( monsterObject == null )
			{
				continue;
			}

			// Don't check if bot is online til we can do it safely for large numbers of can_faxbot requests
			//if ( !FaxRequestFrame.isBotOnline( botName ) )
			//{
			//	continue;
			//}
			return DataTypes.TRUE_VALUE;
		}
		return DataTypes.FALSE_VALUE;
	}

	// Major functions which provide item-related
	// information.

	public static Value get_inventory( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		AdventureResult [] items = new AdventureResult[ KoLConstants.inventory.size() ];
		KoLConstants.inventory.toArray( items );

		for ( int i = 0; i < items.length; ++i )
		{
			value.aset(
				DataTypes.makeItemValue( items[i].getItemId(), true ),
				new Value( items[i].getCount() ) );
		}

		return value;
	}

	public static Value get_closet( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		AdventureResult [] items = new AdventureResult[ KoLConstants.closet.size() ];
		KoLConstants.closet.toArray( items );

		for ( int i = 0; i < items.length; ++i )
		{
			value.aset(
				DataTypes.makeItemValue( items[i].getItemId(), true ),
				new Value( items[i].getCount() ) );
		}

		return value;
	}

	public static Value get_storage( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		AdventureResult [] items = new AdventureResult[ KoLConstants.storage.size() ];
		KoLConstants.storage.toArray( items );

		for ( int i = 0; i < items.length; ++i )
		{
			value.aset(
				DataTypes.makeItemValue( items[i].getItemId(), true ),
				new Value( items[i].getCount() ) );
		}

		return value;
	}

	public static Value get_free_pulls( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		AdventureResult [] items = new AdventureResult[ KoLConstants.freepulls.size() ];
		KoLConstants.freepulls.toArray( items );

		for ( int i = 0; i < items.length; ++i )
		{
			value.aset(
				DataTypes.makeItemValue( items[i].getItemId(), true ),
				new Value( items[i].getCount() ) );
		}

		return value;
	}

	public static Value get_shop( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		if ( !KoLCharacter.hasStore() )
		{
			return value;
		}

		if ( !StoreManager.soldItemsRetrieved )
		{
			RequestThread.postRequest( new ManageStoreRequest() );
		}

		List<SoldItem> list = StoreManager.getSoldItemList();
		for ( int i = 0; i < list.size(); ++i )
		{
			SoldItem item = list.get( i );
			value.aset( DataTypes.makeItemValue( item.getItemId(), true ),
				    new Value( item.getQuantity() ) );
		}

		return value;
	}

	public static Value get_shop_log( Interpreter interpreter )
	{
		if ( !KoLCharacter.hasStore() )
		{
			return new Value( DataTypes.STRING_TYPE );
		}
		RequestThread.postRequest( new ManageStoreRequest(true) );
		List<StoreManager.StoreLogEntry> list = StoreManager.getStoreLog();
		AggregateValue value =
			new ArrayValue( new AggregateType( DataTypes.STRING_TYPE, list.size() ) );
		for ( int i = 0; i < list.size(); i++ )
		{
			StoreManager.StoreLogEntry sle = list.get( i );
			if ( sle != null )
			{
				value.aset( new Value( i ), new Value( sle.toString() ) );
			}
		}
		return value;
	}

	public static Value get_stash( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		if ( !KoLCharacter.hasStore() )
		{
			return value;
		}

		List<AdventureResult> list = ClanManager.getStash();
		for ( int i = 0; i < list.size(); ++i )
		{
			AdventureResult item = list.get( i );
			value.aset( DataTypes.makeItemValue( item.getItemId(), true ),
				    new Value( item.getCount() ) );
		}

		return value;
	}

	public static Value get_campground( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		if ( KoLCharacter.inNuclearAutumn() )
		{
			for ( AdventureResult item : KoLConstants.falloutShelter )
			{
				value.aset(
					DataTypes.makeItemValue( item.getItemId(), true ),
					new Value( item.getCount() ) );
			}
		}
		else
		{
			// Your dwelling is not in the list of campground items
			AdventureResult dwelling = CampgroundRequest.getCurrentDwelling();
			value.aset( DataTypes.makeItemValue( dwelling.getItemId(), true ), DataTypes.ONE_VALUE );

			for ( AdventureResult item : KoLConstants.campground )
			{
				value.aset(
					DataTypes.makeItemValue( item.getItemId(), true ),
					new Value( item.getCount() ) );
			}
		}

		return value;
	}

	public static Value get_clan_lounge( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		for ( AdventureResult item : ClanManager.getClanLounge() )
		{
			value.aset(
				DataTypes.makeItemValue( item.getItemId(), true ),
				new Value( item.getCount() ) );
		}

		return value;
	}

	public static Value get_clan_rumpus( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.STRING_TO_INT_TYPE );

		for ( String name : ClanManager.getClanRumpus() )
		{
			int count = 1;
			int countIndex = name.indexOf( " (" );
			if ( countIndex != -1 )
			{
				count = StringUtilities.parseInt( name.substring( countIndex + 2, name.length() - 1 ) );
				name = name.substring( 0, countIndex );
			}
			value.aset(
				new Value( name ),
				new Value( count ) );
		}

		return value;
	}

	public static Value get_chateau( Interpreter interpreter )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		for ( AdventureResult item : KoLConstants.chateau )
		{
			value.aset(
				DataTypes.makeItemValue( item.getItemId(), true ),
				new Value( item.getCount() ) );
		}

		return value;
	}

	public static Value get_dwelling( Interpreter interpreter )
	{
		return DataTypes.makeItemValue( CampgroundRequest.getCurrentDwelling().getItemId(), true );
	}

	private static final int WAD2POWDER = -12;	// <elem> powder - <elem> wad
	private static final int WAD2NUGGET = -6;
	private static final int WAD2GEM = 1321;

	public static Value get_related( Interpreter interpreter, Value item, Value type )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );
		String which = type.toString();

		if ( which.equals( "zap" ) )
		{
			String[] zapgroup = ZapRequest.getZapGroup( (int) item.intValue() );
			for ( int i = zapgroup.length - 1; i >= 0; --i )
			{
				Value key = DataTypes.parseItemValue( zapgroup[ i ], true );
				if ( key.intValue() != item.intValue() )
				{
					value.aset( key, DataTypes.ZERO_VALUE );
				}
			}
		}
		else if ( which.equals( "fold" ) )
		{
			ArrayList list = ItemDatabase.getFoldGroup( item.toString() );
			if ( list == null ) return value;
			// Don't use element 0, that's the damage percentage
			for ( int i = list.size() - 1; i > 0; --i )
			{
				value.aset(
					DataTypes.parseItemValue( (String) list.get( i ), true ),
					new Value( i ) );
			}
		}
		else if ( which.equals( "pulverize" ) )
		{	// All values scaled up by one million
			int pulver = EquipmentDatabase.getPulverization( (int) item.intValue() );
			if ( pulver == -1 || (pulver & EquipmentDatabase.MALUS_UPGRADE) != 0 )
			{
				return value;
			}
			if ( pulver > 0 )
			{
				value.aset( DataTypes.makeItemValue( pulver, true ),
					    DataTypes.makeIntValue( 1000000 ) );
				return value;
			}

			ArrayList<Integer> elems = new ArrayList<Integer>();
			boolean clusters = ( pulver & EquipmentDatabase.YIELD_1C ) != 0;
			if ( (pulver & EquipmentDatabase.ELEM_HOT) != 0 )
			{
				elems.add( IntegerPool.get( clusters ? ItemPool.HOT_CLUSTER : ItemPool.HOT_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_COLD) != 0 )
			{
				elems.add( IntegerPool.get( clusters ? ItemPool.COLD_CLUSTER : ItemPool.COLD_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_STENCH) != 0 )
			{
				elems.add( IntegerPool.get( clusters ? ItemPool.STENCH_CLUSTER : ItemPool.STENCH_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_SPOOKY) != 0 )
			{
				elems.add( IntegerPool.get( clusters ? ItemPool.SPOOKY_CLUSTER : ItemPool.SPOOKY_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_SLEAZE) != 0 )
			{
				elems.add( IntegerPool.get( clusters ? ItemPool.SLEAZE_CLUSTER : ItemPool.SLEAZE_WAD ) );
			}
			if ( (pulver & EquipmentDatabase.ELEM_TWINKLY) != 0 )
			{	// Important: twinkly must be last
				elems.add( IntegerPool.get( ItemPool.TWINKLY_WAD ) );
			}
			int nelems = elems.size();
			if ( nelems == 0 )
			{
				return value;	// shouldn't happen
			}

			int powders = 0, nuggets = 0, wads = 0;
			if ( (pulver & EquipmentDatabase.YIELD_3W) != 0 )
			{
				wads = 3000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_1W3N_2W) != 0 )
			{
				wads = 1500000;
				nuggets = 1500000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_4N_1W) != 0 )
			{
				wads = 500000;
				nuggets = 2000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_3N) != 0 )
			{
				nuggets = 3000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_1N3P_2N) != 0 )
			{
				nuggets = 1500000;
				powders = 1500000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_4P_1N) != 0 )
			{
				nuggets = 500000;
				powders = 2000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_3P) != 0 )
			{
				powders = 3000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_2P) != 0 )
			{
				powders = 2000000;
			}
			else if ( (pulver & EquipmentDatabase.YIELD_1P) != 0 )
			{
				powders = 1000000;
			}
			int gems = wads / 100;
			wads -= gems;

			for ( int wad : elems )
			{
				if ( powders > 0 )
				{
					value.aset( DataTypes.makeItemValue( wad + WAD2POWDER, true ),
						    DataTypes.makeIntValue( powders / nelems ) );
				}
				if ( nuggets > 0 )
				{
					value.aset( DataTypes.makeItemValue( wad + WAD2NUGGET, true ),
						    DataTypes.makeIntValue( nuggets / nelems ) );
				}
				if ( wads > 0 )
				{
					if ( wad == ItemPool.TWINKLY_WAD )
					{	// no twinkly gem!
						wads += gems;
						gems = 0;
					}
					value.aset( DataTypes.makeItemValue( wad, true ),
						    DataTypes.makeIntValue( wads / nelems ) );
				}
				if ( gems > 0 )
				{
					value.aset( DataTypes.makeItemValue( wad + WAD2GEM, true ),
						    DataTypes.makeIntValue( gems / nelems ) );
				}
				if ( clusters )
				{
					value.aset( DataTypes.makeItemValue( wad, true ),
						    DataTypes.makeIntValue( 1000000 ) );
				}
			}
		}
		return value;
	}

	public static Value is_tradeable( Interpreter interpreter, final Value item )
	{
		return DataTypes.makeBooleanValue( ItemDatabase.isTradeable( (int) item.intValue() ) );
	}

	public static Value is_giftable( Interpreter interpreter, final Value item )
	{
		return DataTypes.makeBooleanValue( ItemDatabase.isGiftable( (int) item.intValue() ) );
	}

	public static Value is_displayable( Interpreter interpreter, final Value item )
	{
		int itemId = (int) item.intValue();
		return DataTypes.makeBooleanValue( !ItemDatabase.isQuestItem( itemId ) && !ItemDatabase.isVirtualItem( itemId ) );
	}

	public static Value is_discardable( Interpreter interpreter, final Value item )
	{
		return DataTypes.makeBooleanValue( ItemDatabase.isDiscardable( (int) item.intValue() ) );
	}

	public static Value is_npc_item( Interpreter interpreter, final Value item )
	{
		return DataTypes.makeBooleanValue( NPCStoreDatabase.contains( (int) item.intValue(), false ) );
	}

	public static Value is_coinmaster_item( Interpreter interpreter, final Value item )
	{
		return DataTypes.makeBooleanValue( CoinmastersDatabase.contains( (int) item.intValue(), false ) );
	}

	public static Value autosell_price( Interpreter interpreter, final Value item )
	{
		return new Value( ItemDatabase.getPriceById( (int) item.intValue() ) );
	}

	public static Value mall_price( Interpreter interpreter, final Value item )
	{
		return new Value( StoreManager.getMallPrice( ItemPool.get( (int) item.intValue(), 0 ) ) );
	}

	public static Value mall_prices( Interpreter interpreter, final Value arg )
	{
		if ( arg.getType().equals( DataTypes.STRING_TYPE ) )
		{
			return new Value( StoreManager.getMallPrices( arg.toString(), "" ) );
		}

		// It's a set of items
		AggregateValue aggregate = (AggregateValue)arg;
		int size = aggregate.count();
		Value [] keys = aggregate.keys();
		AdventureResult [] itemIds = new AdventureResult[ size ];

		// Extract the item ids into an array
		for ( int i = 0; i < size; ++i )
		{
			Value item = keys[ i ];
			itemIds[ i ] = ItemPool.get( (int) item.contentLong, 1 );
		}

		// Update the mall prices, one by one, 
		int result = StoreManager.getMallPrices( itemIds, 0.0f );

		return DataTypes.makeIntValue( result );
	}

	public static Value mall_prices( Interpreter interpreter, final Value category, final Value tiers )
	{
		return new Value( StoreManager.getMallPrices( category.toString(), tiers.toString() ) );
	}

	public static Value npc_price( Interpreter interpreter, final Value item )
	{
		int itemId = (int) item.intValue();
		String it = ItemDatabase.getCanonicalName( itemId );
		return new Value(
			NPCStoreDatabase.contains( itemId, true ) ?
			NPCStoreDatabase.price( itemId ) :
			ClanLoungeRequest.availableSpeakeasyDrink( it ) ?
			ClanLoungeRequest.speakeasyNameToCost( it ).intValue() :
			0 );
	}
 
	public static Value shop_price( Interpreter interpreter, final Value item )
	{
		if ( !KoLCharacter.hasStore() )
		{
			return DataTypes.ZERO_VALUE;
		}

		if ( !StoreManager.soldItemsRetrieved )
		{
			RequestThread.postRequest( new ManageStoreRequest() );
		}

		return new Value( StoreManager.getPrice( (int) item.intValue() ) );
	}

	// Coinmaster functions

	public static Value buys_item( Interpreter interpreter, final Value master, final Value item )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		return DataTypes.makeBooleanValue( data != null && data.canSellItem( (int) item.intValue() ) );
	}

	public static Value buy_price( Interpreter interpreter, final Value master, final Value item )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		return DataTypes.makeIntValue( data != null ? data.getSellPrice( (int) item.intValue() ) : 0 );
	}

	public static Value sells_item( Interpreter interpreter, final Value master, final Value item )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		return DataTypes.makeBooleanValue( data != null && data.canBuyItem( (int) item.intValue() ) );
	}

	public static Value sell_price( Interpreter interpreter, final Value master, final Value item )
	{
		CoinmasterData data = (CoinmasterData) master.rawValue();
		return DataTypes.makeIntValue( data != null ? data.getBuyPrice( (int) item.intValue() ) : 0 );
	}

	public static Value historical_price( Interpreter interpreter, final Value item )
	{
		return new Value( MallPriceDatabase.getPrice( (int) item.intValue() ) );
	}

	public static Value historical_age( Interpreter interpreter, final Value item )
	{
		return new Value( MallPriceDatabase.getAge( (int) item.intValue() ) );
	}

	public static Value daily_special( Interpreter interpreter )
	{
		AdventureResult special =
			KoLCharacter.gnomadsAvailable() ? MicroBreweryRequest.getDailySpecial() : KoLCharacter.canadiaAvailable() ? ChezSnooteeRequest.getDailySpecial() : null;

		return special == null ? DataTypes.ITEM_INIT : DataTypes.makeItemValue( special.getItemId(), true );
	}

	public static Value refresh_shop( Interpreter interpreter )
	{
		RequestThread.postRequest( new ManageStoreRequest() );
		return RuntimeLibrary.continueValue();
	}

	public static Value refresh_stash( Interpreter interpreter )
	{
		RequestThread.postRequest( new ClanStashRequest() );
		return RuntimeLibrary.continueValue();
	}

	public static Value available_amount( Interpreter interpreter, final Value arg )
	{
		AdventureResult item = ItemPool.get( (int) arg.intValue(), 0 );
		return DataTypes.makeIntValue( InventoryManager.getAccessibleCount( item ) );
	}

	public static Value item_amount( Interpreter interpreter, final Value arg )
	{
		AdventureResult item = ItemPool.get( (int) arg.intValue(), 0 );
		return new Value( item.getCount( KoLConstants.inventory ) );
	}

	public static Value closet_amount( Interpreter interpreter, final Value arg )
	{
		AdventureResult item = ItemPool.get( (int) arg.intValue(), 0 );
		return new Value( item.getCount( KoLConstants.closet ) );
	}

	public static Value equipped_amount( Interpreter interpreter, final Value arg )
	{
		AdventureResult item = ItemPool.get( (int) arg.intValue(), 0 );
		int runningTotal = 0;

		for ( int i = 0; i <= EquipmentManager.FAMILIAR; ++i )
		{
			if ( EquipmentManager.getEquipment( i ).equals( item ) )
			{
				++runningTotal;
			}
		}

		return new Value( runningTotal );
	}

	public static Value creatable_amount( Interpreter interpreter, final Value arg )
	{
		CreateItemRequest item = CreateItemRequest.getInstance( (int) arg.intValue() );
		return new Value( item == null ? 0 : item.getQuantityPossible() );
	}

	public static Value creatable_turns( Interpreter interpreter, final Value itemId )
	{
		AdventureResult item = ItemPool.get( (int) itemId.intValue() );
		if ( item == null )
		{
			return new Value( 0 );
		}
		int initialAmount = item.getCount( KoLConstants.inventory );
		Concoction concoction = ConcoctionPool.get( item );
		return new Value( concoction == null ? 0 : concoction.getAdventuresNeeded( initialAmount + 1 ) );
	}

	public static Value creatable_turns( Interpreter interpreter, final Value itemId, final Value count )
	{
		AdventureResult item = ItemPool.get( (int) itemId.intValue() );
		int number = (int) count.intValue();
		if ( item == null )
		{
			return new Value( 0 );
		}
		int initialAmount = item.getCount( KoLConstants.inventory );
		Concoction concoction = ConcoctionPool.get( item );
		return new Value( concoction == null ? 0 : concoction.getAdventuresNeeded( initialAmount + number ) );
	}

	public static Value creatable_turns( Interpreter interpreter, final Value itemId, final Value count, final Value freeCrafting )
	{
		AdventureResult item = ItemPool.get( (int) itemId.intValue() );
		int number = (int) count.intValue();
		boolean considerFreeCrafting = freeCrafting.intValue() == 1;
		if ( item == null )
		{
			return new Value( 0 );
		}
		int initialAmount = item.getCount( KoLConstants.inventory );
		Concoction concoction = ConcoctionPool.get( item );
		return new Value( concoction == null ? 0 : concoction.getAdventuresNeeded( initialAmount + number, considerFreeCrafting ) );
	}

	public static Value get_ingredients( Interpreter interpreter, final Value arg )
	{
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		int itemId = (int) arg.intValue();
		CraftingType method = ConcoctionDatabase.getMixingMethod( itemId );
		EnumSet<CraftingRequirements> requirements = ConcoctionDatabase.getRequirements( itemId );
		if ( !ConcoctionDatabase.isPermittedMethod( method, requirements ) )
		{
			return value;	// can't make it
		}

		AdventureResult[] data = ConcoctionDatabase.getIngredients( itemId );
		for ( int i = 0; i < data.length; ++i )
		{
			AdventureResult ingredient = data[ i ];
			if ( ingredient.getItemId() < 0 )
			{
				// Skip pseudo-ingredients: coinmaster tokens
				continue;
			}
			int count = ingredient.getCount();
			Value key = DataTypes.makeItemValue( ingredient.getItemId(), true );
			if ( value.contains( key ) )
			{
				count += (int) value.aref( key ).intValue();
			}
			value.aset( key, new Value( count ) );
		}

		return value;
	}

	public static Value storage_amount( Interpreter interpreter, final Value arg )
	{
		AdventureResult item = ItemPool.get( (int) arg.intValue(), 0 );
		return new Value( item.getCount( KoLConstants.storage ) + item.getCount( KoLConstants.freepulls ) );
	}

	public static Value display_amount( Interpreter interpreter, final Value arg )
	{
		if ( !KoLCharacter.hasDisplayCase() )
		{
			return DataTypes.ZERO_VALUE;
		}

		if ( !DisplayCaseManager.collectionRetrieved )
		{
			RequestThread.postRequest( new DisplayCaseRequest() );
		}

		AdventureResult item = ItemPool.get( (int) arg.intValue(), 0 );
		return new Value( item.getCount( KoLConstants.collection ) );
	}

	public static Value shop_amount( Interpreter interpreter, final Value arg )
	{
		if ( !KoLCharacter.hasStore() )
		{
			return DataTypes.ZERO_VALUE;
		}

		if ( !StoreManager.soldItemsRetrieved )
		{
			RequestThread.postRequest( new ManageStoreRequest() );
		}

		SoldItem item = new SoldItem( (int) arg.intValue(), 0, 0, 0, 0 );

		List<SoldItem> list = StoreManager.getSoldItemList();
		int index = list.indexOf( item );
		if ( index < 0 )
		{
			return DataTypes.ZERO_VALUE;
		}

		item = (SoldItem) list.get( index );
		return new Value( item.getQuantity() );
	}

	public static Value stash_amount( Interpreter interpreter, final Value arg )
	{
		if ( !ClanManager.stashRetrieved )
		{
			RequestThread.postRequest( new ClanStashRequest() );
		}

		List stash = ClanManager.getStash();

		AdventureResult item = ItemPool.get( (int) arg.intValue(), 0 );
		return new Value( item.getCount( stash ) );
	}

	public static Value pulls_remaining( Interpreter interpreter )
	{
		return new Value( ConcoctionDatabase.getPullsRemaining() );
	}

	public static Value stills_available( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getStillsAvailable() );
	}

	public static Value have_mushroom_plot( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( MushroomManager.ownsPlot() );
	}

	public static Value craft_type( Interpreter interpreter, final Value arg )
	{
		int itemId = (int) arg.intValue();
		Concoction conc = ConcoctionPool.get( itemId );
		if ( conc == null )
		{
			return new Value( "none" );
		}
		CraftingType method = conc.getMixingMethod();
		EnumSet<CraftingRequirements> requirements = conc.getRequirements();
		return new Value( ConcoctionDatabase.mixingMethodDescription( method, requirements ) );
	}

	// The following functions pertain to providing updated
	// information relating to the player.

	public static Value refresh_status( Interpreter interpreter )
	{
		ApiRequest.updateStatus();
		return RuntimeLibrary.continueValue();
	}

	public static Value restore_hp( Interpreter interpreter, final Value amount )
	{
		return RuntimeLibrary.restore( true, (int) amount.intValue() );
	}

	public static Value restore_mp( Interpreter interpreter, final Value amount )
	{
		return RuntimeLibrary.restore( false, (int) amount.intValue() );
	}

	private static Value restore( boolean hp, int amount )
	{
		boolean wasRecoveryActive = RecoveryManager.isRecoveryActive();
		try
		{
			RecoveryManager.setRecoveryActive( true );
			SpecialOutfit.createImplicitCheckpoint();
			return DataTypes.makeBooleanValue( hp ? RecoveryManager.recoverHP( amount ) : RecoveryManager.recoverMP( amount ) );
		}
		finally
		{
			SpecialOutfit.restoreImplicitCheckpoint();
			RecoveryManager.setRecoveryActive( wasRecoveryActive );
		}
	}

	public static Value mood_execute( Interpreter interpreter, final Value multiplicity )
	{
		if ( RecoveryManager.isRecoveryActive() || MoodManager.isExecuting() )
		{
			return DataTypes.VOID_VALUE;
		}

		try
		{
			SpecialOutfit.createImplicitCheckpoint();
			MoodManager.execute( (int) multiplicity.intValue() );
			return DataTypes.VOID_VALUE;
		}
		finally
		{
			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	public static Value my_name( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getUserName() );
	}

	public static Value my_id( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getPlayerId() );
	}

	public static Value my_hash( Interpreter interpreter )
	{
		return new Value( GenericRequest.passwordHash );
	}

	public static Value my_sign( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getSign() );
	}

	public static Value my_path( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getPath() );
	}

	public static Value in_muscle_sign( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.inMuscleSign() );
	}

	public static Value in_mysticality_sign( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.inMysticalitySign() );
	}

	public static Value in_moxie_sign( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.inMoxieSign() );
	}

	public static Value in_bad_moon( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.inBadMoon() );
	}

	public static Value my_class( Interpreter interpreter )
	{
		return DataTypes.makeClassValue( KoLCharacter.getClassType() );
	}

	public static Value my_level( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getLevel() );
	}

	public static Value my_hp( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getCurrentHP() );
	}

	public static Value my_maxhp( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getMaximumHP() );
	}

	public static Value my_mp( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getCurrentMP() );
	}

	public static Value my_maxmp( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getMaximumMP() );
	}

	public static Value my_primestat( Interpreter interpreter )
	{
		int primeIndex = KoLCharacter.getPrimeIndex();
		return primeIndex == 0 ? DataTypes.MUSCLE_VALUE : primeIndex == 1 ? DataTypes.MYSTICALITY_VALUE : DataTypes.MOXIE_VALUE;
	}

	public static Value my_basestat( Interpreter interpreter, final Value arg )
	{
		String stat = arg.toString();

		if ( stat.equalsIgnoreCase( Stat.MUSCLE.toString() ) )
		{
			return new Value( KoLCharacter.getBaseMuscle() );
		}
		if ( stat.equalsIgnoreCase( Stat.MYSTICALITY.toString() ) )
		{
			return new Value( KoLCharacter.getBaseMysticality() );
		}
		if ( stat.equalsIgnoreCase( Stat.MOXIE.toString() ) )
		{
			return new Value( KoLCharacter.getBaseMoxie() );
		}

		if ( stat.equalsIgnoreCase( Stat.SUBMUSCLE.toString() ) )
		{
			return new Value( KoLCharacter.getTotalMuscle() );
		}
		if ( stat.equalsIgnoreCase( Stat.SUBMYST.toString() ) )
		{
			return new Value( KoLCharacter.getTotalMysticality() );
		}
		if ( stat.equalsIgnoreCase( Stat.SUBMOXIE.toString() ) )
		{
			return new Value( KoLCharacter.getTotalMoxie() );
		}

		return DataTypes.ZERO_VALUE;
	}

	public static Value my_buffedstat( Interpreter interpreter, final Value arg )
	{
		String stat = arg.toString();

		if ( stat.equalsIgnoreCase( Stat.MUSCLE.toString() ) )
		{
			return new Value( KoLCharacter.getAdjustedMuscle() );
		}
		if ( stat.equalsIgnoreCase( Stat.MYSTICALITY.toString() ) )
		{
			return new Value( KoLCharacter.getAdjustedMysticality() );
		}
		if ( stat.equalsIgnoreCase( Stat.MOXIE.toString() ) )
		{
			return new Value( KoLCharacter.getAdjustedMoxie() );
		}

		return DataTypes.ZERO_VALUE;
	}

	public static Value my_fury( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getFury() );
	}

	public static Value my_maxfury( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getFuryLimit() );
	}

	public static Value my_soulsauce( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getSoulsauce() );
	}

	public static Value my_discomomentum( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getDiscoMomentum() );
	}

	public static Value my_audience( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getAudience() );
	}

	public static Value my_absorbs( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getAbsorbs() );
	}

	public static Value my_thunder( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getThunder() );
	}

	public static Value my_rain( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getRain() );
	}

	public static Value my_lightning( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getLightning() );
	}

	public static Value my_mask( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getMask() );
	}

	public static Value my_meat( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getAvailableMeat() );
	}

	public static Value my_closet_meat( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getClosetMeat() );
	}

	public static Value my_storage_meat( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getStorageMeat() );
	}

	public static Value my_session_meat( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getSessionMeat() );
	}

	public static Value my_adventures( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getAdventuresLeft() );
	}

	public static Value my_session_adv( Interpreter interpreter )
	{
		int adv = 0;
		for ( AdventureResult result : KoLConstants.tally )
		{
			if ( result.getConditionType().equals( "choiceadv" ) )
			{
				adv = result.getCount();
			}
		}
		return new Value( adv );
	}

	public static Value my_daycount( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getCurrentDays() );
	}

	public static Value my_turncount( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getCurrentRun() );
	}

	public static Value my_fullness( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getFullness() );
	}

	public static Value fullness_limit( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getFullnessLimit() );
	}

	public static Value my_inebriety( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getInebriety() );
	}

	public static Value inebriety_limit( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getInebrietyLimit() );
	}

	public static Value my_spleen_use( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getSpleenUse() );
	}

	public static Value spleen_limit( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getSpleenLimit() );
	}

	public static Value can_eat( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.canEat() );
	}

	public static Value can_drink( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.canDrink() );
	}

	public static Value turns_played( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getCurrentRun() );
	}

	public static Value total_turns_played( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getTurnsPlayed() );
	}

	public static Value my_ascensions( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getAscensions() );
	}

	public static Value can_interact( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.canInteract() );
	}

	public static Value in_hardcore( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.isHardcore() );
	}

	public static Value pvp_attacks_left( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getAttacksLeft() );
	}

	public static Value current_pvp_stances( Interpreter interpreter )
	{
		AggregateType type = new AggregateType( DataTypes.INT_TYPE, DataTypes.STRING_TYPE );
		MapValue value = new MapValue( type );

		if ( PvpManager.checkStances() )
		{
			for ( Entry<String, Integer> entry : PvpManager.stanceToOption.entrySet() )
			{
				value.aset( new Value( entry.getKey() ), new Value( entry.getValue() ) );
			}
		}

		return value;
	}

	public static Value get_clan_id( Interpreter interpreter )
	{
		return new Value( ClanManager.getClanId() );
	}

	public static Value get_clan_name( Interpreter interpreter )
	{
		return new Value( ClanManager.getClanName( true ) );
	}

	public static Value limit_mode( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getLimitmode() );
	}

	public static Value get_florist_plants( Interpreter interpreter )
	{
		AggregateType plantType = new AggregateType( DataTypes.STRING_TYPE, 3 );

		AggregateType type = new AggregateType( plantType, DataTypes.LOCATION_TYPE );
		MapValue value = new MapValue( type );

		Set locations = FloristRequest.floristPlants.keySet();
		Iterator iterator = locations.iterator();
		while ( iterator.hasNext() )
		{
			String loc = (String) iterator.next();
			Value location = DataTypes.parseLocationValue( loc, false );
			if ( location == null )
			{
				// The location string from KoL couldn't be
				// matched to a location in KoLmafia
				continue;
			}
			ArrayList<Florist> plants = FloristRequest.getPlants( loc );
			if ( plants.size() == 0 )
			{
				continue;
			}

			ArrayValue plantValue = new ArrayValue( plantType );
			for ( int i = 0; i < plants.size(); i++ )
			{
				plantValue.aset( new Value( i ), new Value( plants.get( i ).toString() ) );
			}
			value.aset( location, plantValue );
		}

		return value;
	}

	public static Value total_free_rests( Interpreter interpreter )
	{
		return new Value( KoLCharacter.freeRestsAvailable() );
	}

	public static Value get_ignore_zone_warnings( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.getIgnoreZoneWarnings() );
	}

	// Basic skill and effect functions, including those used
	// in custom combat consult scripts.

	public static Value have_skill( Interpreter interpreter, final Value arg )
	{
		String skillName = SkillDatabase.getSkillName( (int) arg.intValue() );
		UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance( skillName );
		return DataTypes.makeBooleanValue( KoLCharacter.hasSkill( skill, KoLConstants.availableSkills ) ||
						   KoLCharacter.hasSkill( skill, KoLConstants.availableCombatSkills ) );
	}

	public static Value mp_cost( Interpreter interpreter, final Value skill )
	{
		return new Value( SkillDatabase.getMPConsumptionById( (int) skill.intValue() ) );
	}

	public static Value adv_cost( Interpreter interpreter, final Value skill )
	{
		return new Value( SkillDatabase.getAdventureCost( (int) skill.intValue() ) );
	}

	public static Value soulsauce_cost( Interpreter interpreter, final Value skill )
	{
		return new Value( SkillDatabase.getSoulsauceCost( (int) skill.intValue() ) );
	}

	public static Value thunder_cost( Interpreter interpreter, final Value skill )
	{
		return new Value( SkillDatabase.getThunderCost( (int) skill.intValue() ) );
	}

	public static Value rain_cost( Interpreter interpreter, final Value skill )
	{
		return new Value( SkillDatabase.getRainCost( (int) skill.intValue() ) );
	}

	public static Value lightning_cost( Interpreter interpreter, final Value skill )
	{
		return new Value( SkillDatabase.getLightningCost( (int) skill.intValue() ) );
	}

	public static Value fuel_cost( Interpreter interpreter, final Value skill )
	{
		return new Value( SkillDatabase.getFuelCost( (int) skill.intValue() ) );
	}

	public static Value turns_per_cast( Interpreter interpreter, final Value skill )
	{
		return new Value( SkillDatabase.getEffectDuration( (int) skill.intValue() ) );
	}

	public static Value have_effect( Interpreter interpreter, final Value arg )
	{
		if ( arg == DataTypes.EFFECT_INIT )
		{
			return DataTypes.ZERO_VALUE;
		}
		int effectId = (int) arg.intValue();
		AdventureResult effect = EffectPool.get( effectId, 0 );
		return new Value( effect.getCount( KoLConstants.activeEffects ) );
	}

	public static Value my_effects( Interpreter interpreter )
	{
		AdventureResult[] effectsArray = new AdventureResult[ KoLConstants.activeEffects.size() ];
		KoLConstants.activeEffects.toArray( effectsArray );

		AggregateType type = new AggregateType( DataTypes.INT_TYPE, DataTypes.EFFECT_TYPE );
		MapValue value = new MapValue( type );

		for ( int i = 0; i < effectsArray.length; ++i )
		{
			AdventureResult effect = effectsArray[ i ];
			int duration = effect.getCount();
			if ( duration == Integer.MAX_VALUE )
			{
				duration = -1;
			}

			value.aset(
				DataTypes.makeEffectValue( effect.getEffectId(), true ),
				new Value ( duration ) );
		}

		return value;
	}

	public static Value use_skill( Interpreter interpreter, final Value countValue, final Value skill )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		int skillId = (int) skill.intValue();

		if ( SkillDatabase.isCombat( skillId ) )
		{
			// If we are in combat, go ahead and cast using fight.php

			if ( FightRequest.getCurrentRound() > 0 )
			{
				for ( int i = 0; i < count; ++i )
				{
					RuntimeLibrary.use_skill( interpreter, skill );
				}

				return DataTypes.TRUE_VALUE;
			}

			// If we are not in combat, bail if the skill can't be cast

			if ( !SkillDatabase.isNormal( skillId ) )
			{
				return DataTypes.FALSE_VALUE;
			}
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "cast", count + " " + SkillDatabase.getSkillName( (int) skill.intValue() ) );
		return UseSkillRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value use_skill( Interpreter interpreter, final Value skill )
	{
		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		if ( SkillDatabase.isCombat( (int) skill.intValue() ) && FightRequest.getCurrentRound() > 0 )
		{
			return RuntimeLibrary.visit_url( interpreter, "fight.php?action=skill&whichskill=" + (int) skill.intValue() );
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "cast", "1 " + SkillDatabase.getSkillName( (int) skill.intValue() ) );
		return new Value( UseSkillRequest.lastUpdate );
	}

	public static Value use_skill( Interpreter interpreter, final Value countValue, final Value skill, final Value target )
	{
		int count = (int) countValue.intValue();
		if ( count <= 0 )
		{
			return RuntimeLibrary.continueValue();
		}

		// Just in case someone assumed that use_skill would also work
		// in combat, go ahead and allow it here.

		int skillId = (int) skill.intValue();

		if ( SkillDatabase.isCombat( skillId ) )
		{
			// If we are in combat, go ahead and cast using fight.php

			if ( FightRequest.getCurrentRound() > 0 )
			{
				for ( int i = 0; i < count; ++i )
				{
					RuntimeLibrary.use_skill( interpreter, skill );
				}

				return DataTypes.TRUE_VALUE;
			}

			// If we are not in combat, bail if the skill can't be cast

			if ( !SkillDatabase.isNormal( skillId ) )
			{
				return DataTypes.FALSE_VALUE;
			}
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "cast", count + " " + SkillDatabase.getSkillName( (int) skill.intValue() ) + " on " + target );
		return UseSkillRequest.lastUpdate.equals( "" ) ? RuntimeLibrary.continueValue() : DataTypes.FALSE_VALUE;
	}

	public static Value last_skill_message( Interpreter interpreter )
	{
		return new Value( UseSkillRequest.lastUpdate );
	}

	public static Value get_auto_attack( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getAutoAttackAction() );
	}

	public static Value set_auto_attack( Interpreter interpreter, Value attackValue )
	{
		Type type = attackValue.getType();
		String arg =
			type.equals( DataTypes.TYPE_STRING ) ? attackValue.toString() :
			type.equals( DataTypes.TYPE_INT ) ? String.valueOf( attackValue.intValue() ) :
			"none";

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "autoattack", arg );

		return DataTypes.VOID_VALUE;
	}

	public static Value attack( Interpreter interpreter )
	{
		return RuntimeLibrary.visit_url( interpreter, "fight.php?action=attack" );
	}

	public static Value steal( Interpreter interpreter )
	{
		return RuntimeLibrary.visit_url( interpreter, "fight.php?action=steal" );
	}

	public static Value runaway( Interpreter interpreter )
	{
		return RuntimeLibrary.visit_url( interpreter, "fight.php?action=runaway" );
	}

	public static Value throw_item( Interpreter interpreter, final Value item )
	{
		return RuntimeLibrary.visit_url( interpreter, "fight.php?action=useitem&whichitem=" + (int) item.intValue() );
	}

	public static Value throw_items( Interpreter interpreter, final Value item1, final Value item2 )
	{
		return RuntimeLibrary.visit_url( interpreter, "fight.php?action=useitem&whichitem=" + (int) item1.intValue() + "&whichitem2=" + (int) item2.intValue() );
	}

	public static Value run_choice( Interpreter interpreter, final Value decision )
	{
		return run_choice( interpreter, decision, DataTypes.TRUE_VALUE );
	}

	public static Value run_choice( Interpreter interpreter, final Value decision, final Value custom )
	{
		int option = (int) decision.intValue();
		boolean handleFights = custom.intValue() != 0;

		String response = null;
		if ( ( !ChoiceManager.handlingChoice && !FightRequest.choiceFollowsFight ) || ChoiceManager.lastResponseText == null ||
		     option == 0 )
		{
			// If you are not in a choice, or you send 0, just return the last response
			response = ChoiceManager.lastResponseText;
		}
		else if ( option == -1 )
		{
			// Try to automate using existing settings
			response = ChoiceManager.gotoGoal();
		}
		else if ( option > 0 )
		{
			// Submit the option chosen
			String message = "Submitting option " + option + " for choice " + ChoiceManager.getLastChoice();
			RequestLogger.printLine( message );

			// If want to handle fights via CCS, use ChoiceManager.CHOICE_HANDLER
			if ( handleFights )
			{
				response = ChoiceManager.processChoiceAdventure( option, false );
			}
			// Otherwise, submit it in a new GenericRequest
			else
			{
				GenericRequest request = new GenericRequest( "choice.php" );
				request.addFormField( "whichchoice", String.valueOf( ChoiceManager.lastChoice ) );
				request.addFormField( "option", String.valueOf( decision ) );
				request.addFormField( "pwd", GenericRequest.passwordHash );
				request.run();
			}
		}
		return new Value( DataTypes.BUFFER_TYPE, "", new StringBuffer( response == null ? "" : response ) );
	}

	public static Value last_choice( Interpreter interpreter )
	{
		return DataTypes.makeIntValue( ChoiceManager.lastChoice );
	}

	public static Value last_decision( Interpreter interpreter )
	{
		return DataTypes.makeIntValue( ChoiceManager.lastDecision );
	}

	public static Value available_choice_options( Interpreter interpreter )
	{
		return RuntimeLibrary.available_choice_options( false );
	}

	public static Value available_choice_options( Interpreter interpreter, Value spoilers )
	{
		return RuntimeLibrary.available_choice_options( spoilers.intValue() == 1 );
	}

	private static Value available_choice_options( boolean spoilers)
	{
		MapValue value = new MapValue( DataTypes.INT_TO_STRING_TYPE );

		String responseText = ChoiceManager.lastResponseText;
		if ( responseText != null && !responseText.equals( "" ) )
		{
			Map<Integer,String> choices = spoilers ?
				ChoiceUtilities.parseChoicesWithSpoilers() :
				ChoiceUtilities.parseChoices( responseText );

			for ( Entry<Integer,String> entry : choices.entrySet() )
			{
				value.aset( DataTypes.makeIntValue( entry.getKey() ),
					    new Value( entry.getValue() ) );
			}
		}
		
		return value;
	}

	public static Value in_multi_fight( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( FightRequest.inMultiFight );
	}

	public static Value choice_follows_fight( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( FightRequest.choiceFollowsFight );
	}

	public static Value run_combat( Interpreter interpreter )
	{
		RelayRequest relayRequest = interpreter.getRelayRequest();

		if ( FightRequest.currentRound > 0 || FightRequest.inMultiFight )
		{
			RequestThread.postRequest( FightRequest.INSTANCE );
		}
		String response = relayRequest == null ?
			FightRequest.lastResponseText : FightRequest.getNextTrackedRound();

		return new Value( DataTypes.BUFFER_TYPE, "", new StringBuffer( response == null ? "" : response ) );
	}

	public static Value run_combat( Interpreter interpreter, Value filterFunction )
	{
		if ( FightRequest.currentRound == 0 && !FightRequest.inMultiFight )
		{
			return new Value( DataTypes.BUFFER_TYPE, "", new StringBuffer( "" ) );
		}

		try
		{
			String filter = filterFunction.toString();
			Macrofier.setMacroOverride( filter, interpreter );
			RequestThread.postRequest( FightRequest.INSTANCE );
		}
		finally
		{
			Macrofier.resetMacroOverride();
		}

		String response = FightRequest.lastResponseText;

		return new Value( DataTypes.BUFFER_TYPE, "", new StringBuffer( response == null ? "" : response ) );
	}

	public static Value run_turn( Interpreter interpreter )
	{
		if ( FightRequest.currentRound > 0 || FightRequest.inMultiFight )
		{
			return RuntimeLibrary.run_combat( interpreter );
		}
		else if ( ( ChoiceManager.handlingChoice && ChoiceManager.lastResponseText != null ) ||
			FightRequest.choiceFollowsFight )
		{
			return RuntimeLibrary.run_choice( interpreter, new Value( -1 ) );
		}
		return new Value( DataTypes.BUFFER_TYPE, "" );
	}

	public static Value stun_skill( Interpreter interpreter )
	{
		String stunSkill = KoLCharacter.getClassStun();
		int skill = -1;

		if ( !stunSkill.equals( "none" ) )
		{
			skill = SkillDatabase.getSkillId( stunSkill );
		}

		return DataTypes.makeSkillValue( skill, true );
	}

	public static Value reverse_numberology( Interpreter interpreter )
	{
		return reverse_numberology( interpreter, new Value( 0 ), new Value( 0 ) );
	}

	public static Value reverse_numberology( Interpreter interpreter, final Value advDelta, final Value spleenDelta )
	{
		MapValue value = new MapValue( NumberologyType );

		Map<Integer, Integer> map = NumberologyManager.reverseNumberology( (int) advDelta.intValue(), (int) spleenDelta.intValue() );


		for ( Map.Entry<Integer, Integer> e : map.entrySet() )
		{
			value.aset( new Value( e.getKey() ), new Value( e.getValue() ) );
		}

		return value;
	}

	public static Value numberology_prize( Interpreter interpreter, final Value num )
	{
		return DataTypes.makeStringValue( NumberologyManager.numberologyPrize( (int) num.intValue() ) );
	}

	public static Value every_card_name( Interpreter interpreter, final Value name )
	{
		// Use logic from CLI "play" command
		List<String> matchingNames = DeckOfEveryCardRequest.getMatchingNames( name.toString() );
		if ( matchingNames.size() == 0 ||	// No match
		     matchingNames.size() > 1 )		// Ambiguous
		{
			return DataTypes.STRING_INIT;
		}

		EveryCard card = DeckOfEveryCardRequest.canonicalNameToCard( matchingNames.get( 0 ) );
		return ( card == null ) ? DataTypes.STRING_INIT : DataTypes.makeStringValue( card.name );
	}

	// Equipment functions.

	public static Value can_equip( Interpreter interpreter, final Value item )
	{
		int itemId = (int) item.intValue();
		switch ( ItemDatabase.getConsumptionType( itemId ) )
		{
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_CONTAINER:
		case KoLConstants.EQUIP_FAMILIAR:
		case KoLConstants.EQUIP_ACCESSORY:
			break;
		default:
			return DataTypes.FALSE_VALUE;
		}
		
		return DataTypes.makeBooleanValue( EquipmentManager.canEquip( ItemDatabase.getItemName( itemId ) ) );
	}

	public static Value equip( Interpreter interpreter, final Value item )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "equip", "\u00B6" + (int) item.intValue() );
		return RuntimeLibrary.continueValue();
	}

	public static Value equip( Interpreter interpreter, final Value slotValue, final Value item )
	{
		String slot = slotValue.toString();
		if ( item.equals( DataTypes.ITEM_INIT ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "unequip", slot );
		}
		else
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "equip", slot + " \u00B6" + (int) item.intValue() );
		}

		return RuntimeLibrary.continueValue();
	}

	public static Value equipped_item( Interpreter interpreter, final Value slot )
	{
		return DataTypes.makeItemValue( EquipmentManager.getEquipment( (int) slot.intValue() ).getName() );
	}

	public static Value have_equipped( Interpreter interpreter, final Value item )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasEquipped( ItemPool.get( (int) item.intValue() ) ) );
	}

	public static Value outfit( Interpreter interpreter, final Value outfit )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "outfit", outfit.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value have_outfit( Interpreter interpreter, final Value outfit )
	{
		SpecialOutfit so = EquipmentManager.getMatchingOutfit( outfit.toString() );

		if ( so == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		int id = so.getOutfitId();
		return DataTypes.makeBooleanValue( id < 0 || EquipmentManager.hasOutfit( id ) );
	}

	public static Value is_wearing_outfit( Interpreter interpreter, final Value outfit )
	{
		SpecialOutfit so = EquipmentManager.getMatchingOutfit( outfit.toString() );

		if ( so == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		return DataTypes.makeBooleanValue( EquipmentManager.isWearingOutfit( so ) );
	}

	public static Value outfit_pieces( Interpreter interpreter, final Value outfit )
	{
		SpecialOutfit so = EquipmentManager.getMatchingOutfit( outfit.toString() );
		if ( so == null )
		{
			return new ArrayValue( new AggregateType( DataTypes.ITEM_TYPE, 0 ) );
		}

		AdventureResult[] pieces = so.getPieces();
		AggregateType type = new AggregateType( DataTypes.ITEM_TYPE, pieces.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			AdventureResult piece = pieces[ i ];
			value.aset( DataTypes.makeIntValue( i ),
				    DataTypes.makeItemValue( piece ) );
		}

		return value;
	}

	public static Value outfit_tattoo( Interpreter interpreter, final Value outfit )
	{
		SpecialOutfit so = EquipmentManager.getMatchingOutfit( outfit.toString() );

		if ( so == null || so.getImage() == null )
		{
			return DataTypes.STRING_INIT;
		}

		return new Value( so.getImage() );
	}
	
	public static Value get_outfits( Interpreter interpreter )
	{
		return RuntimeLibrary.outfitListToValue( interpreter, EquipmentManager.getOutfits(), false );
	}

	public static Value get_custom_outfits( Interpreter interpreter )
	{
		return RuntimeLibrary.outfitListToValue( interpreter, EquipmentManager.getCustomOutfits(), false );
	}

	public static Value all_normal_outfits( Interpreter interpreter )
	{
		return RuntimeLibrary.outfitListToValue( interpreter, EquipmentDatabase.normalOutfits.toList(), true );
	}

	private static Value outfitListToValue( Interpreter interpreter, List<SpecialOutfit> outfits, boolean map )
	{
		AggregateValue value =
			map ?
			new MapValue( new AggregateType( DataTypes.STRING_TYPE, DataTypes.INT_TYPE ) ) :
			new ArrayValue( new AggregateType( DataTypes.STRING_TYPE, outfits.size() ) );

		for ( int i = 1; i < outfits.size(); ++i )
		{
			SpecialOutfit it = outfits.get( i );
			if ( it != null )
			{
				value.aset( new Value( i ), new Value( it.toString() ) );
			}
		}

		return value;
	}

	public static Value weapon_hands( Interpreter interpreter, final Value item )
	{
		return new Value( EquipmentDatabase.getHands( (int) item.intValue() ) );
	}

	public static Value item_type( Interpreter interpreter, final Value item )
	{
		String type = EquipmentDatabase.getItemType( (int) item.intValue() );
		return new Value( type );
	}

	public static Value weapon_type( Interpreter interpreter, final Value item )
	{
		Stat stat = EquipmentDatabase.getWeaponStat( (int) item.intValue() );
		return stat == Stat.MUSCLE ? DataTypes.MUSCLE_VALUE : stat == Stat.MYSTICALITY ? DataTypes.MYSTICALITY_VALUE :
			stat == Stat.MOXIE ? DataTypes.MOXIE_VALUE : DataTypes.STAT_INIT;
	}

	public static Value get_power( Interpreter interpreter, final Value item )
	{
		return new Value( EquipmentDatabase.getPower( (int) item.intValue() ) );
	}

	public static Value my_familiar( Interpreter interpreter )
	{
		return DataTypes.makeFamiliarValue( KoLCharacter.getFamiliar().getId(), true );
	}

	public static Value my_effective_familiar( Interpreter interpreter )
	{
		return DataTypes.makeFamiliarValue( KoLCharacter.getEffectiveFamiliar().getId(), true );
	}

	public static Value my_enthroned_familiar( Interpreter interpreter )
	{
		return DataTypes.makeFamiliarValue( KoLCharacter.getEnthroned().getId(), true );
	}

	public static Value my_bjorned_familiar( Interpreter interpreter )
	{
		return DataTypes.makeFamiliarValue( KoLCharacter.getBjorned().getId(), true );
	}

	public static Value my_poke_fam( Interpreter interpreter, final Value arg )
	{
		int slot = (int) arg.intValue();
		return DataTypes.makeFamiliarValue( KoLCharacter.getPokeFam( slot ).getId(), true );
	}

	public static Value have_familiar( Interpreter interpreter, final Value familiar )
	{
		int familiarId = (int) familiar.intValue();
		return  familiarId == -1 ?
			DataTypes.FALSE_VALUE :
			DataTypes.makeBooleanValue( KoLCharacter.findFamiliar( familiarId ) != null );
	}

	public static Value use_familiar( Interpreter interpreter, final Value familiar )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "familiar", familiar.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value have_servant( Interpreter interpreter, final Value servant )
	{
		return DataTypes.makeBooleanValue( EdServantData.findEdServant( servant.toString() ) != null );
	}

	public static Value use_servant( Interpreter interpreter, final Value servant )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "servant", servant.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value enthrone_familiar( Interpreter interpreter, final Value familiar )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "enthrone", familiar.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value bjornify_familiar( Interpreter interpreter, final Value familiar )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "bjornify", familiar.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value familiar_equipment( Interpreter interpreter, final Value familiar )
	{
		return DataTypes.makeItemValue( FamiliarDatabase.getFamiliarItemId( (int) familiar.intValue() ), true );
	}

	public static Value familiar_equipped_equipment( Interpreter interpreter, final Value familiar )
	{
		FamiliarData fam = KoLCharacter.findFamiliar( (int) familiar.intValue() );
		AdventureResult item = fam == null ? EquipmentRequest.UNEQUIP : fam.getItem();
		return item == EquipmentRequest.UNEQUIP ? DataTypes.ITEM_INIT : DataTypes.makeItemValue( item.getItemId(), true );
	}

	public static Value familiar_weight( Interpreter interpreter, final Value familiar )
	{
		FamiliarData fam = KoLCharacter.findFamiliar( (int) familiar.intValue() );
		return fam == null ? DataTypes.ZERO_VALUE : new Value( fam.getWeight() );
	}

	public static Value is_familiar_equipment_locked( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( EquipmentManager.familiarItemLocked() );
	}

	public static Value favorite_familiars( Interpreter interpreter )
	{
		AggregateType type = new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.FAMILIAR_TYPE );
		MapValue value = new MapValue( type );

		for ( FamiliarData fam : KoLCharacter.getFamiliarList() )
		{
			if ( fam.getFavorite() )
			{
				value.aset( DataTypes.makeFamiliarValue( fam.getId(), true ), DataTypes.makeBooleanValue( fam.canEquip() ) );
			}
		}

		return value;
	}

	public static Value lock_familiar_equipment( Interpreter interpreter, Value lock )
	{
		if ( ( lock.intValue() == 1 ) != EquipmentManager.familiarItemLocked() )
		{
			RequestThread.postRequest( new FamiliarRequest( true ) );
		}
		return DataTypes.VOID_VALUE;
	}

	public static Value equip_all_familiars( Interpreter interpreter )
	{
		FamiliarManager.equipAllFamiliars();
		return RuntimeLibrary.continueValue();
	}

	public static Value minstrel_level( Interpreter interpreter )
	{
		return DataTypes.makeIntValue( KoLCharacter.getMinstrelLevel() );
	}

	public static Value minstrel_instrument( Interpreter interpreter )
	{
		AdventureResult item = KoLCharacter.getCurrentInstrument();
		return item == null ? DataTypes.ITEM_INIT : DataTypes.makeItemValue( item.getItemId(), true );
	}

	public static Value minstrel_quest( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.minstrelAttention );
	}

	public static Value my_companion( Interpreter interpreter )
	{
		if ( KoLCharacter.getCompanion() == null )
		{
			return DataTypes.STRING_INIT;
		}
		return new Value( KoLCharacter.getCompanion().toString() );
	}

	public static Value my_thrall( Interpreter interpreter )
	{
		return DataTypes.makeThrallValue( KoLCharacter.currentPastaThrall(), true );
	}

	public static Value my_servant( Interpreter interpreter )
	{
		return DataTypes.makeServantValue( EdServantData.currentServant(), true );
	}

	public static Value my_vykea_companion( Interpreter interpreter )
	{
		return DataTypes.makeVykeaValue( VYKEACompanionData.currentCompanion(), true );
	}

	// Random other functions related to current in-game
	// state, not directly tied to the character.

	public static Value council( Interpreter interpreter )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "council", "" );
		return DataTypes.VOID_VALUE;
	}

	public static Value current_mcd( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getMindControlLevel() );
	}

	public static Value change_mcd( Interpreter interpreter, final Value level )
	{
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "mcd", level.toString() );
		return RuntimeLibrary.continueValue();
	}

	public static Value current_rad_sickness( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getRadSickness() );
	}

	public static Value have_chef( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasChef() );
	}

	public static Value have_bartender( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasBartender() );
	}

	public static Value have_shop( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasStore() );
	}

	public static Value have_display( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.hasDisplayCase() );
	}

	public static Value hippy_stone_broken( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.getHippyStoneBroken() );
	}

	public static Value get_counters( Interpreter interpreter, final Value label, final Value min, final Value max )
	{
		return new Value( TurnCounter.getCounters( label.toString(), (int) min.intValue(), (int) max.intValue() ) );
	}

	public static Value stop_counter( Interpreter interpreter, final Value label )
	{
		TurnCounter.stopCounting( label.toString() );
		return DataTypes.VOID_VALUE;
	}

	public static Value eudora ( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getEudora() );
	}

	public static Value eudora( Interpreter interpreter, final Value newEudora )
	{
		String correspondent = newEudora.toString();
		String requestString = "account.php?am=1&action=whichpenpal&ajax=1&pwd=" +
			  GenericRequest.passwordHash + "&value=";

		if ( correspondent.equalsIgnoreCase( "penpal" ) )
		{
			GenericRequest request = new GenericRequest( requestString + "1" );
			request.run();
			ApiRequest.updateStatus();
			if ( KoLCharacter.getEudora().equals( "Penpal" ) )
			{
				KoLmafia.updateDisplay( "Switched to Pen Pal" );
				return DataTypes.TRUE_VALUE;
			}
		}
		if ( correspondent.equalsIgnoreCase( "game" ) )
		{
			GenericRequest request = new GenericRequest( requestString + "2" );
			request.run();
			ApiRequest.updateStatus();
			if ( KoLCharacter.getEudora().equals( "GameInformPowerDailyPro Magazine" ) )
			{
				KoLmafia.updateDisplay( "Switched to Game Magazine" );
				return DataTypes.TRUE_VALUE;
			}
		}
		if ( correspondent.equalsIgnoreCase( "xi" ) )
		{
			GenericRequest request = new GenericRequest( requestString + "3" );
			request.run();
			ApiRequest.updateStatus();
			if ( KoLCharacter.getEudora().equals( "Xi Receiver Unit" ) )
			{
				KoLmafia.updateDisplay( "Switched to Xi Receiver" );
				return DataTypes.TRUE_VALUE;
			}
		}
		if ( correspondent.equalsIgnoreCase( "newyou" ) )
		{
			GenericRequest request = new GenericRequest( requestString + "4" );
			request.run();
			ApiRequest.updateStatus();
			if ( KoLCharacter.getEudora().equals( "New-You Club" ) )
			{
				KoLmafia.updateDisplay( "Switched to New-You Club" );
				return DataTypes.TRUE_VALUE;
			}
		}
		return DataTypes.FALSE_VALUE;
	}

	// String parsing functions.

	public static Value is_integer( Interpreter interpreter, final Value string )
	{
		return DataTypes.makeBooleanValue( StringUtilities.isNumeric( string.toString() ) );
	}

	public static Value contains_text( Interpreter interpreter, final Value source, final Value search )
	{
		return DataTypes.makeBooleanValue( source.toString().contains( search.toString() ) );
	}

	public static Value starts_with( Interpreter interpreter, final Value source, final Value prefix )
	{
		return DataTypes.makeBooleanValue( source.toString().startsWith( prefix.toString() ) );
	}

	public static Value ends_with( Interpreter interpreter, final Value source, final Value suffix )
	{
		return DataTypes.makeBooleanValue( source.toString().endsWith( suffix.toString() ) );
	}

	public static Value extract_meat( Interpreter interpreter, final Value string )
	{
		ArrayList<AdventureResult> data = new ArrayList<AdventureResult>();
		ResultProcessor.processResults( false,
			StringUtilities.globalStringReplace( string.toString(), "- ", "-" ),
			data );

		for ( AdventureResult result : data )
		{
			if ( result.getName().equals( AdventureResult.MEAT ) )
			{
				return new Value( result.getCount() );
			}
		}

		return DataTypes.ZERO_VALUE;
	}

	public static Value extract_items( Interpreter interpreter, final Value string )
	{
		ArrayList<AdventureResult> data = new ArrayList<AdventureResult>();
		ResultProcessor.processResults( false,
			StringUtilities.globalStringReplace( string.toString(), "- ", "-" ),
			data );
		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );

		for ( AdventureResult result : data )
		{
			if ( result.isItem() )
			{
				value.aset(
					DataTypes.makeItemValue( result.getItemId(), true ),
					DataTypes.parseIntValue( String.valueOf( result.getCount() ), true ) );
			}
		}

		return value;
	}

	public static Value length( Interpreter interpreter, final Value string )
	{
		return new Value( string.toString().length() );
	}

	public static Value char_at( Interpreter interpreter, final Value source, final Value index )
	{
		String string = source.toString();
		int offset = (int) index.intValue();
		if ( offset < 0 || offset >= string.length() )
		{
			throw interpreter.runtimeException( "Offset " + offset + " out of bounds" );
		}
		return new Value( Character.toString( string.charAt( offset ) ) );
	}

	public static Value index_of( Interpreter interpreter, final Value source, final Value search )
	{
		String string = source.toString();
		String substring = search.toString();
		return new Value( string.indexOf( substring ) );
	}

	public static Value index_of( Interpreter interpreter, final Value source, final Value search,
		final Value start )
	{
		String string = source.toString();
		String substring = search.toString();
		int begin = (int) start.intValue();
		if ( begin < 0 || begin > string.length() )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		return new Value( string.indexOf( substring, begin ) );
	}

	public static Value last_index_of( Interpreter interpreter, final Value source, final Value search )
	{
		String string = source.toString();
		String substring = search.toString();
		return new Value( string.lastIndexOf( substring ) );
	}

	public static Value last_index_of( Interpreter interpreter, final Value source, final Value search,
		final Value start )
	{
		String string = source.toString();
		String substring = search.toString();
		int begin = (int) start.intValue();
		if ( begin < 0 || begin > string.length() )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		return new Value( string.lastIndexOf( substring, begin ) );
	}

	public static Value substring( Interpreter interpreter, final Value source, final Value start )
	{
		String string = source.toString();
		int begin = (int) start.intValue();
		if ( begin < 0 || begin > string.length() )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		return new Value( string.substring( begin ) );
	}

	public static Value substring( Interpreter interpreter, final Value source, final Value start,
		final Value finish )
	{
		String string = source.toString();
		int begin = (int) start.intValue();
		if ( begin < 0 )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		int end = (int) finish.intValue();
		if ( end > string.length() )
		{
			throw interpreter.runtimeException( "End index " + end + " out of bounds" );
		}
		if ( begin > end )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " greater than end index " + end );
		}
		return new Value( string.substring( begin, end ) );
	}

	public static Value to_upper_case( Interpreter interpreter, final Value string )
	{
		return new Value( string.toString().toUpperCase() );
	}

	public static Value to_lower_case( Interpreter interpreter, final Value string )
	{
		return new Value( string.toString().toLowerCase() );
	}

	public static Value leetify( Interpreter interpreter, final Value string )
	{
		return new Value( StringUtilities.leetify( string.toString() ) );
	}

	public static Value append( Interpreter interpreter, final Value buffer, final Value s )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		current.append( s.toString() );
		return buffer;
	}

	public static Value insert( Interpreter interpreter, final Value buffer, final Value index, final Value s )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		int offset = (int) index.intValue();
		if ( offset < 0 || offset > current.length() )
		{
			throw interpreter.runtimeException( "Index " + index + " out of bounds" );
		}
		current.insert( offset, s.toString() );
		return buffer;
	}

	public static Value replace( Interpreter interpreter, final Value buffer, final Value start, final Value finish,
		final Value s )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		int begin = (int) start.intValue();
		if ( begin < 0 )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		int end = (int) finish.intValue();
		if ( end > current.length() )
		{
			throw interpreter.runtimeException( "End index " + end + " out of bounds" );
		}
		if ( begin > end )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " greater than end index " + end );
		}
		current.replace( begin, end, s.toString() );
		return buffer;
	}

	public static Value delete( Interpreter interpreter, final Value buffer, final Value start, final Value finish )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		int begin = (int) start.intValue();
		if ( begin < 0 )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " out of bounds" );
		}
		int end = (int) finish.intValue();
		if ( end > current.length() )
		{
			throw interpreter.runtimeException( "End index " + end + " out of bounds" );
		}
		if ( begin > end )
		{
			throw interpreter.runtimeException( "Begin index " + begin + " greater than end index " + end );
		}
		current.delete( begin, end );
		return buffer;
	}

	public static Value set_length( Interpreter interpreter, final Value buffer, final Value i )
	{
		StringBuffer current = (StringBuffer) buffer.rawValue();
		int length = (int) i.intValue();
		if ( length < 0 )
		{
			throw interpreter.runtimeException( "Desired length is less than zero" );
		}
		current.setLength( length );
		return DataTypes.VOID_VALUE;
	}

	public static Value append_tail( Interpreter interpreter, final Value matcher, final Value buffer )
	{
		Matcher m = (Matcher) matcher.rawValue();
		StringBuffer current = (StringBuffer) buffer.rawValue();
		m.appendTail( current );
		return buffer;
	}

	public static Value append_replacement( Interpreter interpreter, final Value matcher, final Value buffer, final Value replacement )
	{
		Matcher m = (Matcher) matcher.rawValue();
		StringBuffer current = (StringBuffer) buffer.rawValue();
		m.appendReplacement( current, replacement.toString() );
		return buffer;
	}

	public static Value create_matcher( Interpreter interpreter, final Value patternValue, final Value stringValue )
	{
		String pattern = patternValue.toString();
		String string = stringValue.toString();

		if ( !( patternValue.content instanceof Pattern ) )
		{
			try
			{
				patternValue.content = Pattern.compile( pattern, Pattern.DOTALL );
			}
			catch ( PatternSyntaxException e )
			{
				throw interpreter.runtimeException( "Invalid pattern syntax" );
			}
		}

		Pattern p = (Pattern) patternValue.content;
		return new Value( DataTypes.MATCHER_TYPE, pattern, p.matcher( string ) );
	}

	public static Value find( Interpreter interpreter, final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		return DataTypes.makeBooleanValue( m.find() );
	}

	public static Value start( Interpreter interpreter, final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		try
		{
			return new Value( m.start() );
		}
		catch ( IllegalStateException e )
		{
			throw interpreter.runtimeException( "No match attempted or previous match failed" );
		}
	}

	public static Value start( Interpreter interpreter, final Value matcher, final Value group )
	{
		Matcher m = (Matcher) matcher.rawValue();
		int index = (int) group.intValue();
		try
		{
			return new Value( m.start( index ) );
		}
		catch ( IllegalStateException e )
		{
			throw interpreter.runtimeException( "No match attempted or previous match failed" );
		}
		catch ( IndexOutOfBoundsException e )
		{
			throw interpreter.runtimeException( "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups" );
		}
	}

	public static Value end( Interpreter interpreter, final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		try
		{
			return new Value( m.end() );
		}
		catch ( IllegalStateException e )
		{
			throw interpreter.runtimeException( "No match attempted or previous match failed" );
		}
	}

	public static Value end( Interpreter interpreter, final Value matcher, final Value group )
	{
		Matcher m = (Matcher) matcher.rawValue();
		int index = (int) group.intValue();
		try
		{
			return new Value( m.end( index ) );
		}
		catch ( IllegalStateException e )
		{
			throw interpreter.runtimeException( "No match attempted or previous match failed" );
		}
		catch ( IndexOutOfBoundsException e )
		{
			throw interpreter.runtimeException( "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups" );
		}
	}

	public static Value group( Interpreter interpreter, final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		try
		{
			return new Value( m.group() );
		}
		catch ( IllegalStateException e )
		{
			throw interpreter.runtimeException( "No match attempted or previous match failed" );
		}
	}

	public static Value group( Interpreter interpreter, final Value matcher, final Value group )
	{
		Matcher m = (Matcher) matcher.rawValue();
		int index = (int) group.intValue();
		try
		{
			return new Value( m.group( index ) );
		}
		catch ( IllegalStateException e )
		{
			throw interpreter.runtimeException( "No match attempted or previous match failed" );
		}
		catch ( IndexOutOfBoundsException e )
		{
			throw interpreter.runtimeException( "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups" );
		}
	}

	public static Value group_count( Interpreter interpreter, final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		return new Value( m.groupCount() );
	}

	public static Value replace_first( Interpreter interpreter, final Value matcher, final Value replacement )
	{
		Matcher m = (Matcher) matcher.rawValue();
		return new Value( m.replaceFirst( replacement.toString() ) );
	}

	public static Value replace_all( Interpreter interpreter, final Value matcher, final Value replacement )
	{
		Matcher m = (Matcher) matcher.rawValue();
		return new Value( m.replaceAll( replacement.toString() ) );
	}

	public static Value reset( Interpreter interpreter, final Value matcher )
	{
		Matcher m = (Matcher) matcher.rawValue();
		m.reset();
		return matcher;
	}

	public static Value reset( Interpreter interpreter, final Value matcher, final Value input )
	{
		Matcher m = (Matcher) matcher.rawValue();
		m.reset( input.toString() );
		return matcher;
	}

	public static Value replace_string( Interpreter interpreter, final Value source,
					    final Value searchValue,
					    final Value replaceValue )
	{
		StringBuffer buffer;
		Value returnValue;

		if ( source.rawValue() instanceof StringBuffer )
		{
			buffer = (StringBuffer) source.rawValue();
			returnValue = source;
		}
		else
		{
			buffer = new StringBuffer( source.toString() );
			returnValue = new Value( DataTypes.BUFFER_TYPE, "", buffer );
		}

		String search = searchValue.toString();
		String replace = replaceValue.toString();

		StringUtilities.globalStringReplace( buffer, search, replace );
		return returnValue;
	}

	public static Value split_string( Interpreter interpreter, final Value string )
	{
		String[] pieces = string.toString().split( KoLConstants.LINE_BREAK );

		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, pieces.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			value.aset( new Value( i ), new Value( pieces[ i ] ) );
		}

		return value;
	}

	public static Value split_string( Interpreter interpreter, final Value string, final Value regex )
	{
		Pattern p;
		if ( regex.rawValue() instanceof Pattern )
		{
			p = (Pattern) regex.rawValue();
		}
		else
		{
			try
			{
				p = Pattern.compile( regex.toString() );
				if ( regex.content == null )
				{
					regex.content = p;
				}
			}
			catch ( PatternSyntaxException e )
			{
				throw interpreter.runtimeException( "Invalid pattern syntax" );
			}
		}
		String[] pieces = p.split( string.toString() );

		AggregateType type = new AggregateType( DataTypes.STRING_TYPE, pieces.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < pieces.length; ++i )
		{
			value.aset( new Value( i ), new Value( pieces[ i ] ) );
		}

		return value;
	}

	public static Value group_string( Interpreter interpreter, final Value string, final Value regex )
	{
		Pattern p;
		if ( regex.rawValue() instanceof Pattern )
		{
			p = (Pattern) regex.rawValue();
		}
		else
		{
			try
			{
				p = Pattern.compile( regex.toString() );
				if ( regex.content == null )
				{
					regex.content = p;
				}
			}
			catch ( PatternSyntaxException e )
			{
				throw interpreter.runtimeException( "Invalid pattern syntax" );
			}
		}
		Matcher userPatternMatcher = p.matcher( string.toString() );
		MapValue value = new MapValue( DataTypes.REGEX_GROUP_TYPE );

		int matchCount = 0;
		int groupCount = userPatternMatcher.groupCount();

		Value[] groupIndexes = new Value[ groupCount + 1 ];
		for ( int i = 0; i <= groupCount; ++i )
		{
			groupIndexes[ i ] = new Value( i );
		}

		Value matchIndex;
		CompositeValue slice;

		try
		{
			while ( userPatternMatcher.find() )
			{
				matchIndex = new Value( matchCount );
				slice = (CompositeValue) value.initialValue( matchIndex );

				value.aset( matchIndex, slice );
				for ( int i = 0; i <= groupCount; ++i )
				{
					slice.aset( groupIndexes[ i ], new Value( userPatternMatcher.group( i ) ) );
				}

				++matchCount;
			}
		}
		catch ( Exception e )
		{
			// Because we're doing everything ourselves, this
			// error shouldn't get generated.  Print a stack
			// trace, just in case.

			StaticEntity.printStackTrace( e );
		}

		return value;
	}

	public static Value expression_eval( Interpreter interpreter, final Value expr )
	{
		Expression e;
		if ( expr.content instanceof Expression )
		{
			e = (Expression) expr.content;
		}
		else
		{
			e = new Expression( expr.toString(), "expression_eval()" );
			String errors = e.getExpressionErrors();
			if ( errors != null )
			{
				throw interpreter.runtimeException( errors );
			}

			if ( expr.content == null )
			{
				expr.content = e;
			}
		}
		return RuntimeLibrary.eval( interpreter, e );
	}

	public static Value modifier_eval( Interpreter interpreter, final Value expr )
	{
		ModifierExpression e;
		if ( expr.content instanceof ModifierExpression )
		{
			e = (ModifierExpression) expr.content;
		}
		else
		{
			e = new ModifierExpression( expr.toString(), "modifier_eval()" );
			String errors = e.getExpressionErrors();
			if ( errors != null )
			{
				throw interpreter.runtimeException( errors );
			}
			if ( expr.content == null )
			{
				expr.content = e;
			}
		}
		return RuntimeLibrary.eval( interpreter, e );
	}

	private static Value eval( Interpreter interpreter, final Expression expr )
	{
		try
		{
			return new Value( expr.evalInternal() );
		}
		catch ( Exception e )
		{
			throw interpreter.runtimeException( "Expression evaluation error: " + e.getMessage() );
		}
	}

	public static Value maximize( Interpreter interpreter, final Value maximizerStringValue, final Value isSpeculateOnlyValue )
	{
		return maximize( interpreter, maximizerStringValue, DataTypes.ZERO_VALUE, DataTypes.ZERO_VALUE, isSpeculateOnlyValue );
	}

	public static Value maximize( Interpreter interpreter, final Value maximizerStringValue, final Value maxPriceValue, final Value priceLevelValue, final Value isSpeculateOnlyValue )
	{
		String maximizerString = maximizerStringValue.toString();
		int maxPrice = (int) maxPriceValue.intValue();
		int priceLevel = (int) priceLevelValue.intValue();
		boolean isSpeculateOnly = isSpeculateOnlyValue.intValue() != 0;

		return new Value( Maximizer.maximize( maximizerString, maxPrice, priceLevel, isSpeculateOnly ) );
	}
	
	public static Value maximize( Interpreter interpreter, final Value maximizerStringValue, final Value maxPriceValue,
		final Value priceLevelValue, final Value isSpeculateOnlyValue, final Value showEquipment )
	{
		String maximizerString = maximizerStringValue.toString();
		int maxPrice = (int) maxPriceValue.intValue();
		int priceLevel = (int) priceLevelValue.intValue();
		boolean isSpeculateOnly = isSpeculateOnlyValue.intValue() != 0;
		boolean showEquip = showEquipment.intValue() == 1;

		Maximizer.maximize( maximizerString, maxPrice, priceLevel, isSpeculateOnly );

		List<Boost> m = Maximizer.boosts;

		int lastEquipIndex = 0;

		if ( !showEquip )
		{
			for ( Boost boo : m )
			{
				if ( !boo.isEquipment() )
					break;
				lastEquipIndex++ ;
			}
		}

		AggregateType type = new AggregateType( RuntimeLibrary.maximizerResults, m.size() - lastEquipIndex );
		ArrayValue value = new ArrayValue( type );

		for ( int i = lastEquipIndex; i < m.size(); ++i )
		{
			Boost boo = m.get( i );
			String text = boo.toString();
			String cmd = boo.getCmd();
			Double boost = boo.getBoost();
			AdventureResult arEffect = boo.isEquipment() ? null : boo.getItem();
			AdventureResult arItem = boo.getItem( false );
			String skill = cmd.startsWith( "cast" ) ? UneffectRequest.effectToSkill( arEffect.getName() ) : null;

			// remove the (+ X) from the display text, that info is in the score
			int cutIndex = boo.toString().indexOf( " (" );
			if ( cutIndex != -1 )
			{
				text = text.substring( 0, cutIndex );
			}

			RecordValue rec = (RecordValue) value.aref( new Value( i - lastEquipIndex ) );

			rec.aset( 0, DataTypes.parseStringValue( text ), null );
			rec.aset( 1, DataTypes.parseStringValue( cmd ), null );
			rec.aset( 2, new Value( boost ), null );
			rec.aset( 3, arEffect == null ? DataTypes.EFFECT_INIT : DataTypes.parseEffectValue( arEffect.getName(), true ), null );
			rec.aset( 4, arItem == null ? DataTypes.ITEM_INIT : DataTypes.makeItemValue( arItem.getItemId(), true ), null );
			rec.aset( 5, skill == null ? DataTypes.SKILL_INIT : DataTypes.parseSkillValue( skill, true ), null );
		}

		return value;
	}

	public static Value monster_eval( Interpreter interpreter, final Value expr )
	{
		MonsterExpression e;
		if ( expr.content instanceof MonsterExpression )
		{
			e = (MonsterExpression) expr.rawValue();
		}
		else
		{
			e = new MonsterExpression( expr.toString(), "monster_eval()" );
			String errors = e.getExpressionErrors();
			if ( errors != null )
			{
				throw interpreter.runtimeException( errors );
			}
			if ( expr.content == null )
			{
				expr.content = e;
			}
		}
		return RuntimeLibrary.eval( interpreter, e );
	}

	public static Value is_online( Interpreter interpreter, final Value arg )
	{
		String name = arg.toString();
		return DataTypes.makeBooleanValue( KoLmafia.isPlayerOnline( name ) );
	}

	static final Pattern COUNT_PATTERN = Pattern.compile( "You have (\\d+) " );
	public static Value slash_count( Interpreter interpreter, final Value arg )
	{
		String itemName = ItemDatabase.getItemName( (int)arg.intValue() );
		InternalChatRequest request = new InternalChatRequest( "/count " + itemName );
		RequestThread.postRequest( request );
		Matcher m = RuntimeLibrary.COUNT_PATTERN.matcher( request.responseText );
		return new Value( m.find() ? StringUtilities.parseInt( m.group( 1 ) ) : 0 );
	}

	public static Value chat_macro( Interpreter interpreter, final Value macroValue )
	{
		String macro = macroValue.toString().trim();

		ChatSender.executeMacro( macro );

		return DataTypes.VOID_VALUE;
	}

	public static Value chat_clan( Interpreter interpreter, final Value messageValue )
	{
		String channel = "/clan";
		String message = messageValue.toString().trim();

		ChatSender.sendMessage( channel, message, true );
		return DataTypes.VOID_VALUE;
	}

	public static Value chat_clan( Interpreter interpreter, final Value messageValue, final Value recipientValue )
	{
		String channel = "/" + recipientValue.toString().trim();
		String message = messageValue.toString().trim();

		ChatSender.sendMessage( channel, message, true );
		return DataTypes.VOID_VALUE;
	}

	public static Value chat_private( Interpreter interpreter, final Value recipientValue, final Value messageValue )
	{
		String recipient = recipientValue.toString();

		String message = messageValue.toString();

		if ( message.equals( "" ) || message.startsWith( "/" ) )
		{
			return DataTypes.VOID_VALUE;
		}

		ChatSender.sendMessage( recipient, message, false );

		return DataTypes.VOID_VALUE;
	}

	public static Value chat_notify( Interpreter interpreter, final Value messageValue, final Value colorValue )
	{
		String messageString = StringUtilities.globalStringReplace( messageValue.toString(), "<", "&lt;" );

		String colorString = StringUtilities.globalStringDelete( colorValue.toString(), "\"" );
		colorString = "\"" + colorString + "\"";

		InternalMessage message = new InternalMessage( messageString, colorString );

		ChatPoller.addEntry( message );

		return DataTypes.VOID_VALUE;
	}

	public static Value who_clan( Interpreter interpreter )
	{
		InternalChatRequest request = new InternalChatRequest( "/who clan" );
		List<ChatMessage> chatMessages = ChatSender.sendRequest( request );

		MapValue value = new MapValue( DataTypes.STRING_TO_BOOLEAN_TYPE );
		for ( ChatMessage chatMessage : chatMessages )
		{
			if ( chatMessage instanceof WhoMessage )
			{
				WhoMessage message = (WhoMessage) chatMessage;

				Iterator entryIterator = message.getContacts().entrySet().iterator();
				while ( entryIterator.hasNext() )
				{
					Entry entry = (Entry) entryIterator.next();
					value.aset( new Value( (String) entry.getKey() ) , new Value( entry.getValue() == Boolean.TRUE ) );
				}

				break;
			}
		}

		return value;
	}

	public static Value get_player_id( Interpreter interpreter, final Value playerNameValue )
	{
		String playerName = playerNameValue.toString();

		return new Value( ContactManager.getPlayerId( playerName, true ) );
	}

	public static Value get_player_name( Interpreter interpreter, final Value playerIdValue )
	{
		String playerId = playerIdValue.toString();

		return new Value( ContactManager.getPlayerName(playerId) );
	}

	// Quest completion functions.

	public static Value tavern( Interpreter interpreter )
	{
		int result = TavernManager.locateTavernFaucet();
		return new Value( KoLmafia.permitsContinue() ? result : -1 );
	}

	public static Value tavern( Interpreter interpreter, final Value arg )
	{
		String goal = arg.toString();
		int result = -1;
		if ( goal.equalsIgnoreCase( "faucet" ) )
		{
			result = TavernManager.locateTavernFaucet();
		}
		else if ( goal.equalsIgnoreCase( "baron" ) )
		{
			result = TavernManager.locateBaron();
		}
		else if ( goal.equalsIgnoreCase( "fight" ) )
		{
			result = TavernManager.fightBaron();
		}
		else if ( goal.equalsIgnoreCase( "explore" ) )
		{
			result = TavernManager.exploreTavern();
		}
		return new Value( KoLmafia.permitsContinue() ? result : -1 );
	}

	public static Value hedge_maze( Interpreter interpreter, final Value arg )
	{
		String goal = arg.toString();
		SorceressLairManager.hedgeMazeScript( goal );
		return RuntimeLibrary.continueValue();
	}

	public static Value tower_door( Interpreter interpreter )
	{
		SorceressLairManager.towerDoorScript();
		return RuntimeLibrary.continueValue();
	}

	// Arithmetic utility functions.

	public static Value random( Interpreter interpreter, final Value arg )
	{
		int range = (int) arg.intValue();
		if ( range < 2 )
		{
			throw interpreter.runtimeException( "Random range must be at least 2" );
		}
		return new Value( KoLConstants.RNG.nextInt( range ) );
	}

	public static Value round( Interpreter interpreter, final Value arg )
	{
		return new Value( (long) Math.round( arg.floatValue() ) );
	}

	public static Value truncate( Interpreter interpreter, final Value arg )
	{
		return new Value( (long) arg.floatValue() );
	}

	public static Value floor( Interpreter interpreter, final Value arg )
	{
		return new Value( (long) Math.floor( arg.floatValue() ) );
	}

	public static Value ceil( Interpreter interpreter, final Value arg )
	{
		return new Value( (long) Math.ceil( arg.floatValue() ) );
	}

	public static Value square_root( Interpreter interpreter, final Value val )
	{
		double value = val.floatValue();
		if ( value < 0.0 )
		{
			throw interpreter.runtimeException( "Can't take square root of a negative value" );
		}
		return new Value( Math.sqrt( value ) );
	}

	public static Value min( Interpreter interpreter, final Value arg1, final Value arg2 )
	{
		if ( arg1.getType() == DataTypes.INT_TYPE && arg2.getType() == DataTypes.INT_TYPE )
		{
			return new Value(  Math.min( arg1.toIntValue().intValue(),
						     arg2.toIntValue().intValue() ) );
		}
		return new Value( Math.min( arg1.toFloatValue().floatValue(),
					    arg2.toFloatValue().floatValue() ) );

	}

	public static Value max( Interpreter interpreter, final Value arg1, final Value arg2 )
	{
		if ( arg1.getType() == DataTypes.INT_TYPE && arg2.getType() == DataTypes.INT_TYPE )
		{
			return new Value( Math.max( arg1.toIntValue().intValue(),
						    arg2.toIntValue().intValue() ) );
		}
		return new Value( Math.max( arg1.toFloatValue().floatValue(),
					    arg2.toFloatValue().floatValue() ) );

	}

	// Settings-type functions.

	public static Value url_encode( Interpreter interpreter, final Value arg )
	{
		return new Value( GenericRequest.encodeURL( arg.toString() ) );
	}

	public static Value url_decode( Interpreter interpreter, final Value arg )
	{
		return new Value( GenericRequest.decodeField( arg.toString() ) );
	}

	public static Value entity_encode( Interpreter interpreter, final Value arg )
		throws UnsupportedEncodingException
	{
		return new Value( CharacterEntities.escape( arg.toString() ) );
	}

	public static Value entity_decode( Interpreter interpreter, final Value arg )
		throws UnsupportedEncodingException
	{
		return new Value( CharacterEntities.unescape( arg.toString() ) );
	}

	private static boolean built_in_property( String name )
	{
		return  name.startsWith( "choiceAdventure" ) ||
			name.startsWith( "skillBurn" ) ||
			RuntimeLibrary.frameNames.contains( name ) ||
			name.equals( "KoLDesktop" );
	}

	public static Value get_all_properties( Interpreter interpreter, final Value filterValue, final Value globalValue )
	{
		// This returns a map from string -> boolean which is property name -> builtin
		// filter is a substring (ignoring case) of the property name. 
		// If filter is "", all properties in the specified scope are returned.

		String filter = filterValue.toString().trim().toLowerCase();
		boolean all = filter.equals( "" );
		boolean global = globalValue.intValue() != 0;

		// The following create a case-insensitive map. This makes
		// properties sort prettily, but, unfortunately, Preferences
		// really are case sensitive.
		//
		// MapValue value = new MapValue( DataTypes.STRING_TO_BOOLEAN_TYPE, true );

		MapValue value = new MapValue( DataTypes.STRING_TO_BOOLEAN_TYPE );

		Map<String, String> properties = Preferences.getMap( false, !global );
		Map<String, String> defaults = Preferences.getMap( true, !global );

		for ( String name : properties.keySet() )
		{
			if ( !Preferences.isUserEditable( name ) )
			{
				continue;
			}

			if ( all || name.toLowerCase().contains( filter ) )
			{
				boolean builtIn = defaults.containsKey( name );
				if ( !builtIn )
				{
					if ( global )
					{
						builtIn = Preferences.isPerUserGlobalProperty( name );
					}
					else
					{
						builtIn = RuntimeLibrary.built_in_property( name );
					}
				}
				Value key = new Value( name );
				Value val = DataTypes.makeBooleanValue( builtIn );
				value.aset( key, val );
			}
		}

		return value;
	}

	public static Value property_exists( Interpreter interpreter, final Value nameValue )
	{
		// Look up a property (in the specified scope) and return true
		// if is present and false otherwise
		String name = nameValue.toString();

		if ( Preferences.propertyExists( name, true ) || Preferences.propertyExists( name, false ) )
		{
			return DataTypes.TRUE_VALUE;
		}

		// All choiceAdventureXXX and skillBurnXXX properties are
		// considered to exist in the user scope even if they don't
		// appear in defaults.txt.

		if ( RuntimeLibrary.built_in_property( name ) )
		{
			return DataTypes.TRUE_VALUE;
		}

		return DataTypes.FALSE_VALUE;
	}

	public static Value property_exists( Interpreter interpreter, final Value nameValue, final Value globalValue )
	{
		// Look up a property (in the specified scope) and return true
		// if is present and false otherwise
		String name = nameValue.toString();
		boolean global = globalValue.intValue() != 0;

		if ( Preferences.propertyExists( name, global ) )
		{
			return DataTypes.TRUE_VALUE;
		}

		// All choiceAdventureXXX and skillBurnXXX properties are
		// considered to exist in the user scope even if they don't
		// appear in defaults.txt.

		if ( !global && RuntimeLibrary.built_in_property( name ) )
		{
			return DataTypes.TRUE_VALUE;
		}
			
		return DataTypes.FALSE_VALUE;
	}

	public static Value property_has_default( Interpreter interpreter, final Value nameValue )
	{
		String name = nameValue.toString();
		return DataTypes.makeBooleanValue( Preferences.containsDefault( name ) );
	}

	public static Value property_default_value( Interpreter interpreter, final Value nameValue )
	{
		String name = nameValue.toString();
		return  Preferences.containsDefault( name ) ?
			DataTypes.makeStringValue( Preferences.getDefault( name ) ) :
			DataTypes.STRING_INIT;
	}

	public static Value get_property( Interpreter interpreter, final Value name )
	{
		String property = name.toString();

		if ( property.startsWith( "System." ) )
		{
			return new Value( System.getProperty( property.substring( 7 ) ) );
		}

		if ( Preferences.isUserEditable( property ) )
		{
			return DataTypes.makeStringValue( Preferences.getString( property ) );
		}

		return DataTypes.STRING_INIT;
	}

	public static Value get_property( Interpreter interpreter, final Value name, final Value globalValue )
	{
		String property = name.toString();

		if ( !Preferences.isUserEditable( property ) )
		{
			return DataTypes.STRING_INIT;
		}

		boolean global = globalValue.intValue() != 0;

		// Look up a property (in the specified scope) and return the current value.
		if ( Preferences.propertyExists( property, global ) )
		{
			return DataTypes.makeStringValue( Preferences.getString( property, global ) );
		}

		// If the property is not found (in the specified scope), "" is returned
		return DataTypes.STRING_INIT;
	}

	public static Value set_property( Interpreter interpreter, final Value nameValue, final Value value )
	{
		// Avoid code duplication for combat related settings
		String name = nameValue.toString();
		boolean builtin = Preferences.containsDefault( name );
		SetPreferencesCommand.setProperty( name, value.toString(), builtin );
		return DataTypes.VOID_VALUE;
	}

	public static Value remove_property( Interpreter interpreter, final Value nameValue )
	{
		String name = nameValue.toString();

		if ( !Preferences.isUserEditable( name ) || Preferences.isPerUserGlobalProperty( name ) )
		{
			return DataTypes.STRING_INIT;
		}

		String oldValue;

		// If it is listed in defaults.txt, set property back to default value.
		if ( Preferences.containsDefault( name ) )
		{
			oldValue = Preferences.getString( name );
			Preferences.resetToDefault( name );
		}
		// If it is in the user map, remove from there
		else if ( Preferences.propertyExists( name, false ) )
		{
			oldValue = Preferences.getString( name, false );
			Preferences.removeProperty( name, false );
		}
		// If it is in the global map, remove from there
		else if ( Preferences.propertyExists( name, true ) )
		{
			oldValue = Preferences.getString( name, true );
			Preferences.removeProperty( name, true );
		}
		// If it's not in either map, nothing to do
		else
		{
			oldValue = "";
		}

		return DataTypes.makeStringValue( oldValue );
	}

	public static Value remove_property( Interpreter interpreter, final Value nameValue, final Value globalValue )
	{
		String name = nameValue.toString();

		if ( !Preferences.isUserEditable( name ) || Preferences.isPerUserGlobalProperty( name ) )
		{
			return DataTypes.STRING_INIT;
		}

		boolean global = globalValue.intValue() != 0;
		String oldValue;

		// If it's not a known global property but we want to remove
		// from global map, we are cleaning up orphaned globals
		if ( !Preferences.isGlobalProperty( name ) && global )
		{
			oldValue = Preferences.getString( name, true );
			Preferences.removeProperty( name, true );
		}
		// If it is a known global property but we want to remove from
		// user map, we are cleaning up orphaned user properties
		else if ( Preferences.isGlobalProperty( name ) && !global )
		{
			oldValue = Preferences.getString( name, false );
			Preferences.removeProperty( name, false );
		}
		// If it is listed in defaults.txt, set property back to default value.
		else if ( Preferences.containsDefault( name ) )
		{
			oldValue = Preferences.getString( name );
			Preferences.resetToDefault( name );
		}
		// Otherwise, remove from the specified map.
		else
		{
			oldValue = Preferences.getString( name, global );
			Preferences.removeProperty( name, global );
		}

		return DataTypes.makeStringValue( oldValue );
	}

	public static Value rename_property( Interpreter interpreter, final Value oldNameValue, final Value newNameValue )
	{
		String oldName = oldNameValue.toString();
		String newName = newNameValue.toString();

		// User scripts cannot rename built-in properties
		if ( Preferences.containsDefault( oldName ) || Preferences.containsDefault( newName ) )
		{
			return DataTypes.FALSE_VALUE;
		}

		// If the old name does not exist, do nothing
		if ( !Preferences.propertyExists( oldName, false ) )
		{
			return DataTypes.FALSE_VALUE;
		}

		// If the new name does exist, do nothing
		if ( Preferences.propertyExists( newName, false ) )
		{
			return DataTypes.FALSE_VALUE;
		}

		// Get value of old property in user map
		String oldValue = Preferences.getString( oldName, false );

		// Remove old property from user map
		Preferences.removeProperty( oldName, false );

		// Create new property in user map
		Preferences.setString( newName, oldValue );

		// Return success
		return DataTypes.TRUE_VALUE;
	}

	// Functions for aggregates.

	public static Value count( Interpreter interpreter, final Value arg )
	{
		return new Value( arg.count() );
	}

	public static Value clear( Interpreter interpreter, final Value arg )
	{
		arg.clear();
		return DataTypes.VOID_VALUE;
	}

	public static Value file_to_map( Interpreter interpreter, final Value var1, final Value var2 )
	{
		return file_to_map( interpreter, var1, var2, DataTypes.TRUE_VALUE );
	}

	public static Value file_to_map( Interpreter interpreter, final Value var1, final Value var2, final Value var3 )
	{
		String filename = var1.toString();
		CompositeValue result = (CompositeValue) var2;
		boolean compact = var3.intValue() == 1;

		BufferedReader reader = DataFileCache.getReader( filename );
		if ( reader == null )
		{
			return DataTypes.FALSE_VALUE;
		}

		String[] data = null;
		result.clear();

		try
		{
			int line = 0;
			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				line++;
				if ( data.length > 1 )
				{
					result.read( data, 0, compact, filename, line );
				}
			}
		}
		catch ( Exception e )
		{
			StringBuilder buffer = new StringBuilder( "Invalid line in data file" );
			if ( data != null )
			{
				buffer.append( ": \"" );
				for ( int i = 0; i < data.length; ++i )
				{
					if ( i > 0 )
					{
						buffer.append( '\t' );
					}
					buffer.append( data[ i ] );
				}
				buffer.append( "\"" );
			}

			// Print the bad data that caused the error
			Exception ex = interpreter.runtimeException( buffer.toString() );

			// If it's a ScriptException, we generated it ourself
			if ( e instanceof ScriptException )
			{
				// Print the bad data and the resulting error
				RequestLogger.printLine( ex.getMessage() );
				RequestLogger.printLine( e.getMessage() );
			}
			else
			{
				// Otherwise, print a stack trace
				StaticEntity.printStackTrace( e, ex.getMessage() );
			}
			return DataTypes.FALSE_VALUE;
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
			}
		}

		return DataTypes.TRUE_VALUE;
	}

	public static Value map_to_file( Interpreter interpreter, final Value var1, final Value var2 )
	{
		return map_to_file( interpreter, var1, var2, DataTypes.TRUE_VALUE );
	}

	public static Value map_to_file( Interpreter interpreter, final Value var1, final Value var2, final Value var3 )
	{
		CompositeValue map_variable = (CompositeValue) var1;
		String filename = var2.toString();
		boolean compact = var3.intValue() == 1;

		ByteArrayOutputStream cacheStream = new ByteArrayOutputStream();

		PrintStream writer = LogStream.openStream( cacheStream, "UTF-8" );
		map_variable.dump( writer, "", compact );
		writer.close();

		byte[] data = cacheStream.toByteArray();
		return DataFileCache.printBytes( filename, data );
	}

	public static Value file_to_array( Interpreter interpreter, final Value var1 )
	{
		String filename = var1.toString();
		MapValue result = new MapValue( DataTypes.INT_TO_STRING_TYPE );

		BufferedReader reader = DataFileCache.getReader( filename );
		if ( reader == null )
		{
			return result;
		}

		String data = null;

		try
		{
			int line = 0;
			while ( ( data = FileUtilities.readLine( reader ) ) != null )
			{
				line++;
				result.aset( new Value( line ), new Value( data ), interpreter );
			}
		}
		catch (Exception e)
		{
			return result;
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
			}
		}

		return result;
	}

	// Custom combat helper functions.

	public static Value my_location( Interpreter interpreter )
	{
		String location = Preferences.getString( "nextAdventure" );
		return DataTypes.parseLocationValue( location, true );
	}

	public static Value set_location( Interpreter interpreter, final Value location )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue();
		KoLAdventure.setNextAdventure( adventure );
		return DataTypes.VOID_VALUE;
	}

	public static Value last_monster( Interpreter interpreter )
	{
		return DataTypes.makeMonsterValue( MonsterStatusTracker.getLastMonster() );
	}

	public static Value get_monsters( Interpreter interpreter, final Value location )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue();
		AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

		int monsterCount = data == null ? 0 : data.getMonsterCount();
		int superlikelyMonsterCount = data == null ? 0 : data.getSuperlikelyMonsterCount();

		AggregateType type = new AggregateType( DataTypes.MONSTER_TYPE, monsterCount + superlikelyMonsterCount );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < monsterCount; ++i )
		{
			value.aset( new Value( i ), DataTypes.makeMonsterValue( data.getMonster( i ) ) );
		}

		for ( int i = 0; i < superlikelyMonsterCount; ++i )
		{
			value.aset( new Value( i + monsterCount ), DataTypes.makeMonsterValue( data.getSuperlikelyMonster( i ) ) );
		}

		return value;

	}

	public static Value get_location_monsters( Interpreter interpreter, final Value location )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue();
		AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

		AggregateType type = new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE );
		MapValue value = new MapValue( type );

		int monsterCount = data == null ? 0 : data.getMonsterCount();
		for ( int i = 0; i < monsterCount; ++i )
		{
			value.aset( DataTypes.makeMonsterValue( data.getMonster( i ) ), DataTypes.TRUE_VALUE );
		}

		int superlikelyMonsterCount = data == null ? 0 : data.getSuperlikelyMonsterCount();
		for ( int i = 0; i < superlikelyMonsterCount; ++i )
		{
			value.aset( DataTypes.makeMonsterValue( data.getSuperlikelyMonster( i ) ), DataTypes.TRUE_VALUE );
		}

		return value;

	}

	public static Value appearance_rates( Interpreter interpreter, final Value location )
	{
		return appearance_rates( interpreter, location, DataTypes.makeBooleanValue( false ) );
	}

	public static Value appearance_rates( Interpreter interpreter, final Value location, final Value includeQueue )
	{
		KoLAdventure adventure = (KoLAdventure) location.rawValue();
		AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

		AggregateType type = new AggregateType( DataTypes.FLOAT_TYPE, DataTypes.MONSTER_TYPE );
		MapValue value = new MapValue( type );
		if ( data == null )
			return value;

		data.recalculate();

		double combatFactor = data.areaCombatPercent();
		value.aset( DataTypes.MONSTER_INIT, new Value( data.combats() < 0 ? -1.0F : 100.0f - combatFactor ) );

		double total = data.totalWeighting();
		double superlikelyChance = 0.0;
		for ( int i = data.getSuperlikelyMonsterCount() - 1; i >= 0; --i )
		{
			MonsterData monster = data.getSuperlikelyMonster( i );
			String name = monster.getName();
			double chance = AreaCombatData.superlikelyChance( name );
			superlikelyChance += chance;
			Value toSet = new Value( chance );
			value.aset( DataTypes.makeMonsterValue( monster ), toSet );
		}
		for ( int i = data.getMonsterCount() - 1; i >= 0; --i )
		{
			int weight = data.getWeighting( i );
			double rejection = data.getRejection( i );
			if ( weight == -2 )
				continue; // impossible this ascension

			Value toSet;
			if ( weight == -4 )
			{
				// Temporarily set to 0% chance
				toSet = new Value( 0 );
			}
			else if ( weight <= 0 )
			{
				// Ultrarares & Banished
				toSet = new Value( weight );
			}
			else if ( includeQueue.intValue() == 1 )
			{
				toSet =
					new Value( AdventureQueueDatabase.applyQueueEffects(
						combatFactor * weight * ( 1 - superlikelyChance / 100 ) * ( 1 - rejection / 100 ), data.getMonster( i ), data ) );
			}
			else
			{
				toSet = new Value( combatFactor * ( 1 - superlikelyChance / 100 ) * ( 1 - rejection / 100 ) * weight / total );
			}
			value.aset( DataTypes.makeMonsterValue( data.getMonster( i ) ), toSet );
		}

		return value;
	}

	public static Value expected_damage( Interpreter interpreter )
	{
		return expected_damage( interpreter, MonsterStatusTracker.getLastMonster(), MonsterStatusTracker.getMonsterAttackModifier() );
	}

	public static Value expected_damage( Interpreter interpreter, final Value arg )
	{
		return expected_damage( interpreter, (MonsterData) arg.rawValue(), 0 );
	}

	private static Value expected_damage( Interpreter interpreter, MonsterData monster, int attackModifier )
	{
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		// http://kol.coldfront.net/thekolwiki/index.php/Damage

		int attack = monster.getAttack() + attackModifier;
		int defenseStat = KoLCharacter.getAdjustedMoxie();

		if ( KoLCharacter.hasSkill(SkillDatabase.getSkillId("Hero of the Half-Shell" ) ) &&
		     EquipmentManager.usingShield() &&
		     KoLCharacter.getAdjustedMuscle() > defenseStat )
		{
			defenseStat = KoLCharacter.getAdjustedMuscle();
		}

		int baseValue =
			Math.max( 0, attack - defenseStat ) + attack / 4 - KoLCharacter.getDamageReduction();

		double damageAbsorb =
			1.0 - ( Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0 ) - 1.0 ) / 10.0;
		double elementAbsorb = 1.0 - KoLCharacter.getElementalResistance( monster.getAttackElement() ) / 100.0;
		return new Value( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
	}

	public static Value monster_level_adjustment( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getMonsterLevelAdjustment() );
	}

	public static Value weight_adjustment( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getFamiliarWeightAdjustment() );
	}

	public static Value mana_cost_modifier( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getManaCostAdjustment() );
	}

	public static Value combat_mana_cost_modifier( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getManaCostAdjustment( true ) );
	}

	public static Value raw_damage_absorption( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getDamageAbsorption() );
	}

	public static Value damage_absorption_percent( Interpreter interpreter )
	{
		int raw = Math.min( 1000, KoLCharacter.getDamageAbsorption() );
		if ( raw == 0 )
		{
			return DataTypes.ZERO_FLOAT_VALUE;
		}

		// http://forums.kingdomofloathing.com/viewtopic.php?p=2016073
		// ( sqrt( raw / 10 ) - 1 ) / 10

		double percent = ( Math.sqrt( raw / 10.0 ) - 1.0 ) * 10.0;
		return new Value( percent );
	}

	public static Value damage_reduction( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getDamageReduction() );
	}

	public static Value elemental_resistance( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getElementalResistance( MonsterStatusTracker.getMonsterAttackElement() ) );
	}

	public static Value elemental_resistance( Interpreter interpreter, final Value arg )
	{
		if ( arg.getType().equals( DataTypes.TYPE_ELEMENT ) )
		{
			String elementName = arg.toString();
			Element elem = Element.fromString( elementName );
			return new Value( KoLCharacter.getElementalResistance( elem ) );
		}

		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( KoLCharacter.getElementalResistance( monster.getAttackElement() ) );
	}

	public static Value combat_rate_modifier( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getCombatRateAdjustment() );
	}

	public static Value initiative_modifier( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getInitiativeAdjustment() );
	}

	public static Value experience_bonus( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getExperienceAdjustment() );
	}

	public static Value meat_drop_modifier( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getMeatDropPercentAdjustment() );
	}

	public static Value item_drop_modifier( Interpreter interpreter )
	{
		return new Value( KoLCharacter.getItemDropPercentAdjustment() );
	}

	public static Value buffed_hit_stat( Interpreter interpreter )
	{
		int hitStat = EquipmentManager.getAdjustedHitStat();
		return new Value( hitStat );
	}

	public static Value current_hit_stat( Interpreter interpreter )
	{
		return EquipmentManager.getHitStatType() == Stat.MOXIE ? DataTypes.MOXIE_VALUE : DataTypes.MUSCLE_VALUE;
	}

	public static Value monster_element( Interpreter interpreter )
	{
		Element element = MonsterStatusTracker.getMonsterDefenseElement();
		return new Value( DataTypes.ELEMENT_TYPE, element.toString(), element );
	}

	public static Value monster_element( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ELEMENT_INIT;
		}

		Element element = monster.getDefenseElement();
		return new Value( DataTypes.ELEMENT_TYPE, element.toString(), element );
	}

	public static Value monster_attack( Interpreter interpreter )
	{
		return new Value( MonsterStatusTracker.getMonsterAttack() );
	}

	public static Value monster_attack( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( monster.getAttack() );
	}

	public static Value monster_defense( Interpreter interpreter )
	{
		return new Value( MonsterStatusTracker.getMonsterDefense() );
	}

	public static Value monster_defense( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( monster.getDefense() );
	}

	public static Value monster_initiative( Interpreter interpreter )
	{
		return new Value( MonsterStatusTracker.getMonsterInitiative() );
	}

	public static Value monster_initiative( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( monster.getInitiative() );
	}

	public static Value monster_hp( Interpreter interpreter )
	{
		return new Value( MonsterStatusTracker.getMonsterHealth() );
	}

	public static Value monster_hp( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.ZERO_VALUE;
		}

		return new Value( monster.getHP() );
	}

	public static Value monster_phylum( Interpreter interpreter )
	{
		Phylum phylum = MonsterStatusTracker.getMonsterPhylum();
		return new Value( DataTypes.PHYLUM_TYPE, phylum.toString(), phylum );
	}

	public static Value monster_phylum( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.PHYLUM_INIT;
		}

		Phylum phylum = monster.getPhylum();
		return new Value( DataTypes.PHYLUM_TYPE, phylum.toString(), phylum );
	}

	public static Value is_banished( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return DataTypes.FALSE_VALUE;
		}
		return DataTypes.makeBooleanValue( BanishManager.isBanished( (String) monster.getName() ) );
	}

	public static Value jump_chance( Interpreter interpreter )
	{
		return new Value( MonsterStatusTracker.getJumpChance() );
	}

	public static Value jump_chance( Interpreter interpreter, final Value arg )
	{
		if ( arg.getType().equals( DataTypes.TYPE_MONSTER ) )
		{
			MonsterData monster = (MonsterData) arg.rawValue();
			if ( monster == null )
			{
				return DataTypes.ZERO_VALUE;
			}
			return new Value( monster.getJumpChance() );
		}
		
		if ( arg.getType().equals( DataTypes.TYPE_LOCATION ) )
		{
			KoLAdventure adventure = (KoLAdventure) arg.rawValue();
			AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

			int monsterCount = data == null ? 0 : data.getMonsterCount();
			int superlikelyMonsterCount = data == null ? 0 : data.getSuperlikelyMonsterCount();
			int jumpChance = data == null ? 0 : 100;
		
			for ( int i = 0; i < monsterCount; ++i )
			{
				int monsterJumpChance = data.getMonster( i ).getJumpChance();
				if ( jumpChance > monsterJumpChance && data.getWeighting( i ) > 0 )
				{
					jumpChance = monsterJumpChance;
				}
			}
			for ( int i = 0; i < superlikelyMonsterCount; ++i )
			{
				MonsterData monster = data.getSuperlikelyMonster( i );
				int monsterJumpChance = monster.getJumpChance();
				if ( jumpChance > monsterJumpChance && data.superlikelyChance( monster.getName() ) > 0 )
				{
					jumpChance = monsterJumpChance;
				}
			}
			return new Value( jumpChance );
		}
		return DataTypes.ZERO_VALUE;
	}

	public static Value jump_chance( Interpreter interpreter, final Value arg, final Value init )
	{
		int initiative = (int) init.intValue();
		if ( arg.getType().equals( DataTypes.TYPE_MONSTER ) )
		{
			MonsterData monster = (MonsterData) arg.rawValue();
			if ( monster == null )
			{
				return DataTypes.ZERO_VALUE;
			}
			return new Value( monster.getJumpChance( initiative ) );
		}
		
		if ( arg.getType().equals( DataTypes.TYPE_LOCATION ) )
		{
			KoLAdventure adventure = (KoLAdventure) arg.rawValue();
			AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

			int monsterCount = data == null ? 0 : data.getMonsterCount();
			int superlikelyMonsterCount = data == null ? 0 : data.getSuperlikelyMonsterCount();
			int jumpChance = data == null ? 0 : 100;
		
			for ( int i = 0; i < monsterCount; ++i )
			{
				int monsterJumpChance = data.getMonster( i ).getJumpChance( initiative );
				if ( jumpChance > monsterJumpChance && data.getWeighting( i ) > 0 )
				{
					jumpChance = monsterJumpChance;
				}
			}
			for ( int i = 0; i < superlikelyMonsterCount; ++i )
			{
				MonsterData monster = data.getSuperlikelyMonster( i );
				int monsterJumpChance = monster.getJumpChance( initiative );
				if ( jumpChance > monsterJumpChance && AreaCombatData.superlikelyChance( monster.getName() ) > 0 )
				{
					jumpChance = monsterJumpChance;
				}
			}
			return new Value( jumpChance );
		}
		return DataTypes.ZERO_VALUE;
	}

	public static Value jump_chance( Interpreter interpreter, final Value arg, final Value init, final Value ml )
	{
		int initiative = (int) init.intValue();
		int monsterLevel = (int) ml.intValue();
		if ( arg.getType().equals( DataTypes.TYPE_MONSTER ) )
		{
			MonsterData monster = (MonsterData) arg.rawValue();
			if ( monster == null )
			{
				return DataTypes.ZERO_VALUE;
			}
			return new Value( monster.getJumpChance( initiative, monsterLevel ) );
		}
		
		if ( arg.getType().equals( DataTypes.TYPE_LOCATION ) )
		{
			KoLAdventure adventure = (KoLAdventure) arg.rawValue();
			AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

			int monsterCount = data == null ? 0 : data.getMonsterCount();
			int jumpChance = data == null ? 0 : 100;
		
			for ( int i = 0; i < monsterCount; ++i )
			{
				int monsterJumpChance = data.getMonster( i ).getJumpChance( initiative, monsterLevel );
				if ( jumpChance > monsterJumpChance && data.getWeighting( i ) > 0 )
				{
					jumpChance = monsterJumpChance;
				}
			}
			return new Value( jumpChance );
		}
		return DataTypes.ZERO_VALUE;
	}

	public static Value item_drops( Interpreter interpreter )
	{
		MonsterData monster = MonsterStatusTracker.getLastMonster();
		List data = monster == null ? new ArrayList() : monster.getItems();

		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );
		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			value.aset(
				DataTypes.makeItemValue( result.getItemId(), true ),
				DataTypes.parseIntValue( String.valueOf( result.getCount() >> 16 ), true ) );
		}

		return value;
	}

	public static Value item_drops( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		List data = monster == null ? new ArrayList() : monster.getItems();

		MapValue value = new MapValue( DataTypes.ITEM_TO_INT_TYPE );
		AdventureResult result;

		for ( int i = 0; i < data.size(); ++i )
		{
			result = (AdventureResult) data.get( i );
			value.aset(
				DataTypes.makeItemValue( result.getItemId(), true ),
				DataTypes.parseIntValue( String.valueOf( result.getCount() >> 16 ), true ) );
		}

		return value;
	}

	public static Value item_drops_array( Interpreter interpreter )
	{
		return item_drops_array( interpreter, MonsterStatusTracker.getLastMonster() );
	}

	public static Value item_drops_array( Interpreter interpreter, final Value arg )
	{
		return item_drops_array( interpreter, (MonsterData) arg.rawValue() );
	}

	public static Value item_drops_array( Interpreter interpreter, MonsterData monster )
	{
		List data = monster == null ? new ArrayList() : monster.getItems();
		int dropCount = data.size();
		AggregateType type = new AggregateType( RuntimeLibrary.itemDropRec, dropCount );
		ArrayValue value = new ArrayValue( type );
		for ( int i = 0; i < dropCount; ++i )
		{
			AdventureResult result = (AdventureResult) data.get( i );
			int count = result.getCount();
			char dropType = (char) (count & 0xFFFF);
			RecordValue rec = (RecordValue) value.aref( new Value( i ) );

			rec.aset( 0, DataTypes.makeItemValue( result.getItemId(), true ), null );
			rec.aset( 1, new Value( count >> 16 ), null );
			if ( dropType < '1' || dropType > '9' )
			{	// leave as an empty string if no special type was given
				rec.aset( 2, new Value( String.valueOf( dropType ) ), null );
			}
		}

		return value;
	}

	public static Value svn_info( Interpreter interpreter, final Value script )
	{
		String[] projects = KoLConstants.SVN_LOCATION.list();
		
		if ( projects == null )
			return getRecInit();

		ArrayList<String> matches = new ArrayList<String>();
		for ( String s: projects )
		{
			if ( s.contains( script.toString() ) )
			{
				matches.add( s );
			}
		}

		if ( matches.size() != 1 )
		{
			return getRecInit();
		}
		File projectFile = new File( KoLConstants.SVN_LOCATION, matches.get( 0 ) );
		try
		{
			if ( !SVNWCUtil.isWorkingCopyRoot( projectFile ) )
			{
				return getRecInit();
			}
		}
		catch ( SVNException e1 )
		{
			return getRecInit();
		}
		RecordType type = RuntimeLibrary.svnInfoRec;
		RecordValue rec = new RecordValue( type );

		// get info

		SVNInfo info;
		try
		{
			info = SVNManager.doInfo( projectFile );
		}
		catch ( SVNException e )
		{
			SVNManager.error( e, null );
			return getRecInit();
		}

		// URL
		rec.aset( 0, new Value( info.getURL().toString() ), null );
		// revision
		rec.aset( 1, DataTypes.makeIntValue( info.getRevision().getNumber() ), null );
		// lastChangedAuthor
		rec.aset( 2, new Value( info.getAuthor() ), null );
		// lastChangedRev
		rec.aset( 3, DataTypes.makeIntValue( info.getCommittedRevision().getNumber() ), null );
		// lastChangedDate
		// use format that is similar to what 'svn info' gives, ex:
		// Last Changed Date: 2003-01-16 23:21:19 -0600 (Thu, 16 Jan 2003)
		SimpleDateFormat SVN_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss z (EEE, dd MMM yyyy)", Locale.US );
		rec.aset( 4, new Value( SVN_FORMAT.format( info.getCommittedDate() ) ), null );
		
		return rec;
	}
	
	private static RecordValue getRecInit()
	{
		RecordType type = RuntimeLibrary.svnInfoRec;
		RecordValue rec = new RecordValue( type );
		rec.aset( 0, DataTypes.STRING_INIT, null );
		// revision
		rec.aset( 1, DataTypes.INT_INIT, null );
		// lastChangedAuthor
		rec.aset( 2, DataTypes.STRING_INIT, null );
		// lastChangedRev
		rec.aset( 3, DataTypes.INT_INIT, null );
		// lastChangedDate
		rec.aset( 4, DataTypes.STRING_INIT, null );

		return rec;
	}

	public static Value meat_drop( Interpreter interpreter )
	{
		MonsterData monster = MonsterStatusTracker.getLastMonster();
		if ( monster == null )
		{
			return new Value( -1 );
		}

		return new Value( monster.getBaseMeat() );
	}

	public static Value meat_drop( Interpreter interpreter, final Value arg )
	{
		MonsterData monster = (MonsterData) arg.rawValue();
		if ( monster == null )
		{
			return new Value( -1 );
		}

		return new Value( monster.getBaseMeat() );
	}

	public static Value will_usually_dodge( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( MonsterStatusTracker.willUsuallyDodge() );
	}

	public static Value will_usually_miss( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( MonsterStatusTracker.willUsuallyMiss() );
	}

	public static Value dad_sea_monkee_weakness( Interpreter interpreter, final Value arg )
	{
		DadManager.Element element = DadManager.weakness( (int)arg.intValue() );
		switch ( element )
		{
		case HOT:
			return new Value( DataTypes.ELEMENT_TYPE, "hot", element );
		case COLD:
			return new Value( DataTypes.ELEMENT_TYPE, "cold", element );
		case STENCH:
			return new Value( DataTypes.ELEMENT_TYPE, "stench", element );
		case SPOOKY:
			return new Value( DataTypes.ELEMENT_TYPE, "spooky", element );
		case SLEAZE:
			return new Value( DataTypes.ELEMENT_TYPE, "sleaze", element );
		}
		return DataTypes.ELEMENT_INIT;
	}

	public static Value flush_monster_manuel_cache( Interpreter interpreter )
	{
		MonsterManuelManager.flushCache();
		return DataTypes.TRUE_VALUE;
	}

	public static Value monster_manuel_text( Interpreter interpreter, final Value arg )
	{
		return new Value( MonsterManuelManager.getManuelText( (int) arg.intValue() ) );
	}

	public static Value monster_factoids_available( Interpreter interpreter, final Value arg1, final Value arg2 )
	{
		return new Value( MonsterManuelManager.getFactoidsAvailable( (int) arg1.intValue(), arg2.intValue() != 0 ) );
	}

	public static Value all_monsters_with_id( Interpreter interpreter )
	{
		AggregateType type = new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE );
		MapValue value = new MapValue( type );

		for ( Integer id : MonsterDatabase.idKeySet() )
		{
			if ( id == 0 ) continue;
			Value v = DataTypes.makeMonsterValue( id, false );
			if ( v != null ) value.aset( v, DataTypes.TRUE_VALUE );
		}

		return value;
	}

	private static String getModifierType( final Value arg )
	{
		Type type = arg.getType();
		String name = arg.toString();
		int id = (int) arg.intValue();
		if ( type.equals( DataTypes.ITEM_TYPE ) )
		{
			return "Item";
		}
		if ( type.equals( DataTypes.EFFECT_TYPE ) )
		{
			return "Effect";
		}
		if ( type.equals( DataTypes.SKILL_TYPE ) )
		{
			return "Skill";
		}
		if ( type.equals( DataTypes.THRALL_TYPE ) )
		{
			return "Thrall";
		}
		if ( name.contains( ":" ) )
		{
			return name.substring( 0, name.indexOf( ":" ) );
		}
		return "Item";
	}

	private static String getModifierName( final Value arg )
	{
		Type type = arg.getType();
		String name = arg.toString();
		int id = (int) arg.intValue();
		if ( type.equals( DataTypes.ITEM_TYPE ) || type.equals( DataTypes.EFFECT_TYPE ) )
		{
			return "[" + id + "]";
		}
		int index = name.indexOf( ":" );
		if ( index != -1 )
		{
			return name.substring( index + 1 );
		}
		return name;
	}

	public static Value numeric_modifier( Interpreter interpreter, final Value modifier )
	{
		String mod = modifier.toString();
		return new Value( KoLCharacter.currentNumericModifier( mod ) );
	}

	public static Value numeric_modifier( Interpreter interpreter, final Value arg, final Value modifier )
	{
		String type = RuntimeLibrary.getModifierType( arg );
		String name = RuntimeLibrary.getModifierName( arg );
		String mod = modifier.toString();
		return new Value( Modifiers.getNumericModifier( type, name, mod ) );
	}

	public static Value numeric_modifier( Interpreter interpreter, final Value familiar, final Value modifier, final Value weight, final Value item )
	{
		FamiliarData fam = new FamiliarData( (int) familiar.intValue() );
		String mod = modifier.toString();
		int w = Math.max( 1, (int) weight.intValue() );
		AdventureResult it = ItemPool.get( (int) item.intValue() );

		return new Value( Modifiers.getNumericModifier( fam, mod, w, it ) );
	}

	public static Value boolean_modifier( Interpreter interpreter, final Value modifier )
	{
		String mod = modifier.toString();
		return DataTypes.makeBooleanValue( KoLCharacter.currentBooleanModifier( mod ) );
	}

	public static Value boolean_modifier( Interpreter interpreter, final Value arg, final Value modifier )
	{
		String type = RuntimeLibrary.getModifierType( arg );
		String name = RuntimeLibrary.getModifierName( arg );
		String mod = modifier.toString();
		return DataTypes.makeBooleanValue( Modifiers.getBooleanModifier( type, name, mod ) );
	}

	public static Value string_modifier( Interpreter interpreter, final Value modifier )
	{
		String mod = modifier.toString();
		return new Value( KoLCharacter.currentStringModifier( mod ) );
	}

	public static Value string_modifier( Interpreter interpreter, final Value arg, final Value modifier )
	{
		String type = RuntimeLibrary.getModifierType( arg );
		String name = RuntimeLibrary.getModifierName( arg );
		String mod = modifier.toString();
		return new Value( Modifiers.getStringModifier( type, name, mod ) );
	}

	public static Value effect_modifier( Interpreter interpreter, final Value arg, final Value modifier )
	{
		String type = RuntimeLibrary.getModifierType( arg );
		String name = RuntimeLibrary.getModifierName( arg );
		String mod = modifier.toString();
		return new Value( DataTypes.parseEffectValue( Modifiers.getStringModifier( type, name, mod ), true ) );
	}

	public static Value class_modifier( Interpreter interpreter, final Value arg, final Value modifier )
	{
		String type = RuntimeLibrary.getModifierType( arg );
		String name = RuntimeLibrary.getModifierName( arg );
		String mod = modifier.toString();
		return new Value( DataTypes.parseClassValue( Modifiers.getStringModifier( type, name, mod ), true ) );
	}

	public static Value skill_modifier( Interpreter interpreter, final Value arg, final Value modifier )
	{
		String type = RuntimeLibrary.getModifierType( arg );
		String name = RuntimeLibrary.getModifierName( arg );
		String mod = modifier.toString();
		return new Value( DataTypes.parseSkillValue( Modifiers.getStringModifier( type, name, mod ), true ) );
	}

	public static Value stat_modifier( Interpreter interpreter, final Value arg, final Value modifier )
	{
		String type = RuntimeLibrary.getModifierType( arg );
		String name = RuntimeLibrary.getModifierName( arg );
		String mod = modifier.toString();
		return new Value( DataTypes.parseStatValue( Modifiers.getStringModifier( type, name, mod ), true ) );
	}

	public static Value white_citadel_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( QuestLogRequest.isWhiteCitadelAvailable() );
	}

	public static Value friars_available( Interpreter interpreter )
	{
		if ( QuestLogRequest.areFriarsAvailable() )
			Preferences.setInteger( "lastFriarCeremonyAscension", Preferences.getInteger( "knownAscensions" ));
		return DataTypes.makeBooleanValue( QuestLogRequest.areFriarsAvailable() );
	}

	public static Value black_market_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( QuestLogRequest.isBlackMarketAvailable() );
	}

	public static Value hippy_store_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( QuestLogRequest.isHippyStoreAvailable() );
	}

	public static Value dispensary_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.getDispensaryOpen() );
	}

	public static Value guild_store_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.getGuildStoreOpen() );
	}

	public static Value hidden_temple_unlocked( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.getTempleUnlocked() );
	}

	public static Value knoll_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.knollAvailable() );
	}

	public static Value canadia_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.canadiaAvailable() );
	}

	public static Value gnomads_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( KoLCharacter.gnomadsAvailable() );
	}

	public static Value florist_available( Interpreter interpreter )
	{
		return DataTypes.makeBooleanValue( FloristRequest.haveFlorist() );
	}

	public static Value is_trendy( Interpreter interpreter, final Value thing )
	{
		// Types: "Items", "Campground", Bookshelf", "Familiars", "Skills", "Clan Item".
		String key = thing.toString();
		Type type = thing.getType();
		boolean result;

		if ( type.equals( DataTypes.TYPE_STRING ) )
		{

			result = TrendyRequest.isTrendy( "Items", key ) &&
				TrendyRequest.isTrendy( "Campground", key ) &&
				TrendyRequest.isTrendy( "Bookshelf", key ) &&
				TrendyRequest.isTrendy( "Familiars", key ) &&
				TrendyRequest.isTrendy( "Skills", key ) &&
				TrendyRequest.isTrendy( "Clan Item", key );
		}
		else if ( type.equals( DataTypes.TYPE_ITEM ) )
		{
			result = TrendyRequest.isTrendy( "Items", key );
		}
		else if ( type.equals( DataTypes.TYPE_FAMILIAR ) )
		{
			result = TrendyRequest.isTrendy( "Familiars", key );
		}
		else if ( type.equals( DataTypes.TYPE_SKILL ) )
		{
			if ( SkillDatabase.isBookshelfSkill( key ) )
			{
				int itemId = SkillDatabase.skillToBook( key );
				key = ItemDatabase.getItemName( itemId );
				result = TrendyRequest.isTrendy( "Bookshelf", key );
			}
			else
			{
				result = TrendyRequest.isTrendy( "Skills", key );
			}
		}
		else
		{
			result = false;
		}

		return DataTypes.makeBooleanValue( result );
	}

	public static Value is_unrestricted( Interpreter interpreter, final Value thing )
	{
		// Types: "Items", Bookshelf Books", "Skills", "Familiars", "Clan Item".
		String key = thing.toString();
		Type type = thing.getType();
		boolean result;

		if ( type.equals( DataTypes.TYPE_STRING ) )
		{
			result =
				StandardRequest.isNotRestricted( "Items", key ) &&
				StandardRequest.isNotRestricted( "Bookshelf Books", key ) &&
				StandardRequest.isNotRestricted( "Skills", key ) &&
				StandardRequest.isNotRestricted( "Familiars", key ) &&
				StandardRequest.isNotRestricted( "Clan Items", key );
		}
		else if ( type.equals( DataTypes.TYPE_ITEM ) )
		{
			result = StandardRequest.isNotRestricted( "Items", key );
		}
		else if ( type.equals( DataTypes.TYPE_FAMILIAR ) )
		{
			result = StandardRequest.isNotRestricted( "Familiars", key );
		}
		else if ( type.equals( DataTypes.TYPE_SKILL ) )
		{
			if ( SkillDatabase.isBookshelfSkill( key ) )
			{
				int itemId = SkillDatabase.skillToBook( key );
				key = ItemDatabase.getItemName( itemId );
				// Work around a KoL bug: most restricted books are
				// listed both under Bookshelf Books and Items, but
				// 3 are listed under only one or the other.
				result = StandardRequest.isNotRestricted( "Bookshelf Books", key ) &&
					 StandardRequest.isNotRestricted( "Items", key );
			}
			else
			{
				result = StandardRequest.isNotRestricted( "Skills", key );
			}
		}
		else
		{
			result = false;
		}

		return DataTypes.makeBooleanValue( result );
	}

	public static Value mmg_visit( Interpreter interpreter )
	{
		RequestThread.postRequest( new MoneyMakingGameRequest() );
		return DataTypes.VOID_VALUE;
	}

	public static Value mmg_search( Interpreter interpreter, final Value arg1, final Value arg2 )
	{
		int lower = (int) arg1.intValue();
		int higher = (int) arg2.intValue();
		RequestThread.postRequest( new MoneyMakingGameRequest( MoneyMakingGameRequest.SEARCH, lower, higher ) );
		return DataTypes.VOID_VALUE;
	}

	public static Value mmg_make_bet( Interpreter interpreter, final Value arg, final Value source )
	{
		int amount = (int) arg.intValue();
		int storage = (int) source.intValue();
		RequestThread.postRequest( new MoneyMakingGameRequest( MoneyMakingGameRequest.MAKE_BET, amount, storage ) );
		return new Value( MoneyMakingGameManager.getLastBetId() );
	}

	public static Value mmg_retract_bet( Interpreter interpreter, final Value arg )
	{
		int id = (int) arg.intValue();
		RequestThread.postRequest( new MoneyMakingGameRequest( MoneyMakingGameRequest.RETRACT_BET, id ) );
		return RuntimeLibrary.continueValue();
	}

	public static Value mmg_take_bet( Interpreter interpreter, final Value arg, final Value source )
	{
		int betId = (int) arg.intValue();
		int storage = (int) source.intValue();
		RequestThread.postRequest( new MoneyMakingGameRequest( MoneyMakingGameRequest.TAKE_BET, betId, storage ) );
		return new Value( MoneyMakingGameManager.getLastWinnings() );
	}

	public static Value mmg_my_bets( Interpreter interpreter )
	{
		int[] bets = MoneyMakingGameManager.getActiveBets();

		AggregateType type = new AggregateType( DataTypes.INT_TYPE, bets.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < bets.length; ++i )
		{
			value.aset( new Value( i ), new Value( bets[ i ] ) );
		}

		return value;
	}

	public static Value mmg_offered_bets( Interpreter interpreter )
	{
		int[] bets = MoneyMakingGameManager.getOfferedBets();

		AggregateType type = new AggregateType( DataTypes.INT_TYPE, bets.length );
		ArrayValue value = new ArrayValue( type );

		for ( int i = 0; i < bets.length; ++i )
		{
			value.aset( new Value( i ), new Value( bets[ i ] ) );
		}

		return value;
	}

	public static Value mmg_bet_owner( Interpreter interpreter, final Value arg )
	{
		int id = (int) arg.intValue();
		return new Value( MoneyMakingGameManager.betOwner( id ) );
	}

	public static Value mmg_bet_owner_id( Interpreter interpreter, final Value arg )
	{
		int id = (int) arg.intValue();
		return new Value( MoneyMakingGameManager.betOwnerId( id ) );
	}

	public static Value mmg_bet_amount( Interpreter interpreter, final Value arg )
	{
		int id = (int) arg.intValue();
		return new Value( MoneyMakingGameManager.betAmount( id ) );
	}

	public static Value mmg_wait_event( Interpreter interpreter, final Value arg )
	{
		int seconds = (int) arg.intValue();
		return new Value( MoneyMakingGameManager.getNextEvent( seconds ) );
	}

	public static Value mmg_bet_taker( Interpreter interpreter )
	{
		return new Value( MoneyMakingGameManager.getLastEventPlayer() );
	}

	public static Value mmg_bet_taker_id( Interpreter interpreter )
	{
		return new Value( MoneyMakingGameManager.getLastEventPlayerId() );
	}

	public static Value mmg_bet_winnings( Interpreter interpreter )
	{
		return new Value( MoneyMakingGameManager.getLastEventWinnings() );
	}

	public static Value svn_exists( Interpreter interpreter, final Value project )
	{
		File f = new File( KoLConstants.SVN_LOCATION, project.toString() );

		boolean isWCRoot = false;
		try
		{
			isWCRoot = SVNWCUtil.isWorkingCopyRoot( f );
		}
		catch ( SVNException e )
		{
			StaticEntity.printStackTrace( e );
		}
		return DataTypes.makeBooleanValue( isWCRoot );
	}

	public static Value svn_at_head( Interpreter interpreter, final Value project )
	{
		File f = new File( KoLConstants.SVN_LOCATION, project.toString() );

		if ( !f.exists() || !f.isDirectory() )
		{
			return DataTypes.FALSE_VALUE;
		}
		return DataTypes.makeBooleanValue( SVNManager.WCAtHead( f, true ) );
	}

	// Sweet Synthesis
	public static Value candy_for_tier( Interpreter interpreter, final Value arg )
	{
		return RuntimeLibrary.candy_for_tier( interpreter, arg, DataTypes.makeIntValue( CandyDatabase.defaultFlags() ) );
	}

	public static Value candy_for_tier( Interpreter interpreter, final Value arg1, final Value arg2 )
	{
		int tier = (int) arg1.intValue();
		int flags = (int) arg2.intValue();
		Set<Integer> candies = CandyDatabase.candyForTier( tier, flags );

		int count = ( candies == null ) ? 0 : candies.size();

		AggregateType type = new AggregateType( DataTypes.ITEM_TYPE, count );
		ArrayValue value = new ArrayValue( type );

		if ( candies != null )
		{
			int index = 0;
			for ( Integer itemId : candies )
			{
				Value key = new Value( index++ );
				Value val = DataTypes.makeItemValue( itemId.intValue(), true );
				value.aset( key, val );
			}
		}

		return value;
	}

	public static Value sweet_synthesis_pairing( Interpreter interpreter, final Value arg1, final Value arg2  )
	{
		return RuntimeLibrary.sweet_synthesis_pairing( interpreter, arg1, arg2, DataTypes.makeIntValue( CandyDatabase.defaultFlags() ) );
	}

	public static Value sweet_synthesis_pairing( Interpreter interpreter, final Value arg1, final Value arg2, final Value arg3  )
	{
		int effectId = (int) arg1.intValue();
		int itemId = (int) arg2.intValue();
		int flags = (int) arg3.intValue();

		Set<Integer> candies = CandyDatabase.sweetSynthesisPairing( effectId, itemId, flags );

		int count = ( candies == null ) ? 0 : candies.size();

		AggregateType type = new AggregateType( DataTypes.ITEM_TYPE, count );
		ArrayValue value = new ArrayValue( type );

		if ( candies != null )
		{
			int index = 0;
			for ( Integer itemId2 : candies )
			{
				Value key = new Value( index++ );
				Value val = DataTypes.makeItemValue( itemId2.intValue(), true );
				value.aset( key, val );
			}
		}

		return value;
	}

	public static Value sweet_synthesis_pair( Interpreter interpreter, final Value arg1  )
	{
		return RuntimeLibrary.sweet_synthesis_pair( interpreter, arg1, DataTypes.makeIntValue( CandyDatabase.defaultFlags() ) );
	}

	public static Value sweet_synthesis_pair( Interpreter interpreter, final Value arg1, final Value arg2  )
	{
		int effectId = (int) arg1.intValue();
		int flags = (int) arg2.intValue();

		AggregateType type = new AggregateType( DataTypes.ITEM_TYPE, 2 );
		ArrayValue value = new ArrayValue( type );

		Candy[] candies = CandyDatabase.synthesisPair( effectId, flags );

		if ( candies.length == 2 )
		{
			value.aset( new Value( 0 ), DataTypes.makeItemValue( candies[0].getItemId(), true ) );
			value.aset( new Value( 1 ), DataTypes.makeItemValue( candies[1].getItemId(), true ) );
		}

		return value;
	}

	public static Value sweet_synthesis_result( Interpreter interpreter, final Value item1, final Value item2 )
	{
		int itemId1 = (int) item1.intValue();
		int itemId2 = (int) item2.intValue();
		int effectId = CandyDatabase.synthesisResult( itemId1, itemId2 );
		return effectId == -1 ? DataTypes.EFFECT_INIT : DataTypes.makeEffectValue( effectId, true );
	}

	public static Value sweet_synthesis( Interpreter interpreter, final Value effect )
	{
		int effectId = (int) effect.intValue();

		Candy[] candies = CandyDatabase.synthesisPair( effectId );

		if ( candies.length != 2 )
		{
			return DataTypes.FALSE_VALUE;
		}

		int itemId1 = candies[0].getItemId();
		int itemId2 = candies[1].getItemId();

		// SweetSynthesisRequest will retrieve the candies
		SweetSynthesisRequest request = new SweetSynthesisRequest( itemId1, itemId2 );
		RequestThread.postRequest( request );
		return RuntimeLibrary.continueValue();
	}

	public static Value sweet_synthesis( Interpreter interpreter, final Value arg1, final Value arg2 )
	{
		Type type = arg1.getType();

		int itemId1 = -1;
		int itemId2 = -1;

		if ( type.equals( DataTypes.TYPE_ITEM ) )
		{
			itemId1 = (int) arg1.intValue();
			itemId2 = (int) arg2.intValue();

			if ( !ItemDatabase.isCandyItem( itemId1 ) || !ItemDatabase.isCandyItem( itemId2 ) )
			{
				return DataTypes.FALSE_VALUE;
			}
		}
		else
		{
			int effectId = (int) arg1.intValue();
			int flags =  (int) arg2.intValue() | CandyDatabase.defaultFlags();

			Candy[] candies = CandyDatabase.synthesisPair( effectId, flags );

			if ( candies.length != 2 )
			{
				return DataTypes.FALSE_VALUE;
			}

			itemId1 = candies[0].getItemId();
			itemId2 = candies[1].getItemId();
		}

		// SweetSynthesisRequest will retrieve the candies
		SweetSynthesisRequest request = new SweetSynthesisRequest( itemId1, itemId2 );
		RequestThread.postRequest( request );
		return RuntimeLibrary.continueValue();
	}

	public static Value get_fuel( Interpreter interpreter )
	{
		return new Value( CampgroundRequest.getFuel() );
	}
}
