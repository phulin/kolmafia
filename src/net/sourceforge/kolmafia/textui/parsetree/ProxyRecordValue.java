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

package net.sourceforge.kolmafia.textui.parsetree;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.PokefamData;
import net.sourceforge.kolmafia.VYKEACompanionData;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.RestoresDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class ProxyRecordValue
	extends RecordValue
{
	public ProxyRecordValue( final RecordType type, final Value obj )
	{
		super( type );

		this.contentLong = obj.contentLong;
		this.contentString = obj.contentString;
		this.content = obj.content;
	}

	@Override
	public Value aref( final Value key, final Interpreter interpreter )
	{
		int index = ( (RecordType) this.type ).indexOf( key );
		if ( index < 0 )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		return this.aref( index, interpreter );
	}

	@Override
	public Value aref( final int index, final Interpreter interpreter )
	{
		RecordType type = (RecordType) this.type;
		int size = type.fieldCount();
		if ( index < 0 || index >= size )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}

		Object rv;
		try
		{
			rv = this.getClass().getMethod( "get_" + type.getFieldNames()[ index ] ).invoke( this );
		}
		catch ( InvocationTargetException e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e.getCause() );
		}
		catch ( Exception e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e );
		}

		if ( rv == null )
		{
			return type.getFieldTypes()[ index ].initialValue();
		}

		if ( rv instanceof Value )
		{
			return (Value) rv;
		}

		if ( rv instanceof Integer )
		{
			return DataTypes.makeIntValue( ( (Integer) rv ).intValue() );
		}

		if ( rv instanceof Double )
		{
			return DataTypes.makeFloatValue( ( (Double) rv ).doubleValue() );
		}

		if ( rv instanceof String )
		{
			return new Value( rv.toString() );
		}

		if ( rv instanceof Boolean )
		{
			return DataTypes.makeBooleanValue( ( (Boolean) rv ).booleanValue() );
		}

		if ( rv instanceof CoinmasterData )
		{
			return DataTypes.makeCoinmasterValue( (CoinmasterData) rv );
		}

		throw interpreter.runtimeException( "Unable to convert attribute value of type: " + rv.getClass() );
	}

	@Override
	public void aset( final Value key, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public void aset( final int index, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public Value remove( final Value key, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public void clear()
	{
	}

	/* Helper for building parallel arrays of field names & types */
	private static class RecordBuilder
	{
		private final ArrayList<String> names;
		private final ArrayList<Type> types;

		public RecordBuilder()
		{
			names = new ArrayList<String>();
			types = new ArrayList<Type>();
		}

		public RecordBuilder add( String name, Type type )
		{
			this.names.add( name.toLowerCase() );
			this.types.add( type );
			return this;
		}

		public RecordType finish( String name )
		{
			int len = this.names.size();
			return new RecordType( name,
				this.names.toArray( new String[len] ),
				this.types.toArray( new Type[len] ) );
		}
	}

	public static class ClassProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "primestat", DataTypes.STAT_TYPE )
			.finish( "class proxy" );

		public ClassProxy( Value obj )
		{
			super( _type, obj );
		}

		public Value get_primestat()
		{
			int primeIndex = KoLCharacter.getPrimeIndex( this.contentString );

			String name = AdventureResult.STAT_NAMES[ primeIndex ];

			return DataTypes.parseStatValue( name, true );
		}
	}

	public static class ItemProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "name", DataTypes.STRING_TYPE )
			.add( "plural", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "smallimage", DataTypes.STRING_TYPE )
			.add( "levelreq", DataTypes.INT_TYPE )
			.add( "quality", DataTypes.STRING_TYPE )
			.add( "adventures", DataTypes.STRING_TYPE )
			.add( "muscle", DataTypes.STRING_TYPE )
			.add( "mysticality", DataTypes.STRING_TYPE )
			.add( "moxie", DataTypes.STRING_TYPE )
			.add( "fullness", DataTypes.INT_TYPE )
			.add( "inebriety", DataTypes.INT_TYPE )
			.add( "spleen", DataTypes.INT_TYPE )
			.add( "minhp", DataTypes.INT_TYPE )
			.add( "maxhp", DataTypes.INT_TYPE )
			.add( "minmp", DataTypes.INT_TYPE )
			.add( "maxmp", DataTypes.INT_TYPE )
			.add( "dailyusesleft", DataTypes.INT_TYPE )
			.add( "notes", DataTypes.STRING_TYPE )
			.add( "quest", DataTypes.BOOLEAN_TYPE )
			.add( "gift", DataTypes.BOOLEAN_TYPE )
			.add( "tradeable", DataTypes.BOOLEAN_TYPE )
			.add( "discardable", DataTypes.BOOLEAN_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "combat_reusable", DataTypes.BOOLEAN_TYPE )
			.add( "usable", DataTypes.BOOLEAN_TYPE )
			.add( "reusable", DataTypes.BOOLEAN_TYPE )
			.add( "multi", DataTypes.BOOLEAN_TYPE )
			.add( "fancy", DataTypes.BOOLEAN_TYPE )
			.add( "candy", DataTypes.BOOLEAN_TYPE )
			.add( "candy_type", DataTypes.STRING_TYPE )
			.add( "chocolate", DataTypes.BOOLEAN_TYPE )
			.add( "seller", DataTypes.COINMASTER_TYPE )
			.add( "buyer", DataTypes.COINMASTER_TYPE )
			.add( "name_length", DataTypes.INT_TYPE )
			.add( "noob_skill", DataTypes.SKILL_TYPE )
			.finish( "item proxy" );

		public ItemProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_name()
		{
			return ItemDatabase.getDataName( (int) this.contentLong );
		}

		public String get_plural()
		{
			return ItemDatabase.getPluralName( (int) this.contentLong );
		}

		public String get_descid()
		{
			return ItemDatabase.getDescriptionId( (int) this.contentLong );
		}

		public String get_image()
		{
			return ItemDatabase.getImage( (int) this.contentLong );
		}

		public String get_smallimage()
		{
			return ItemDatabase.getSmallImage( (int) this.contentLong );
		}

		public Integer get_levelreq()
		{
			return ConsumablesDatabase.getLevelReqByName( this.contentString );
		}

		public String get_quality()
		{
			return ConsumablesDatabase.getQuality( this.contentString );
		}

		public String get_adventures()
		{
			return ConsumablesDatabase.getAdvRangeByName( this.contentString );
		}

		public String get_muscle()
		{
			return ConsumablesDatabase.getMuscleByName( this.contentString );
		}

		public String get_mysticality()
		{
			return ConsumablesDatabase.getMysticalityByName( this.contentString );
		}

		public String get_moxie()
		{
			return ConsumablesDatabase.getMoxieByName( this.contentString );
		}

		public int get_fullness()
		{
			return ConsumablesDatabase.getFullness( this.contentString );
		}

		public int get_inebriety()
		{
			return ConsumablesDatabase.getInebriety( this.contentString );
		}

		public int get_spleen()
		{
			return ConsumablesDatabase.getSpleenHit( this.contentString );
		}

		public int get_minhp()
		{
			return RestoresDatabase.getHPMin( this.contentString );
		}

		public int get_maxhp()
		{
			return RestoresDatabase.getHPMax( this.contentString );
		}

		public int get_minmp()
		{
			return RestoresDatabase.getMPMin( this.contentString );
		}

		public int get_maxmp()
		{
			return RestoresDatabase.getMPMax( this.contentString );
		}

		public int get_dailyusesleft()
		{
			return RestoresDatabase.getUsesLeft( this.contentString );
		}

		public String get_notes()
		{
			return ConsumablesDatabase.getNotes( this.contentString );
		}

		public boolean get_quest()
		{
			return ItemDatabase.isQuestItem( (int) this.contentLong );
		}

		public boolean get_gift()
		{
			return ItemDatabase.isGiftItem( (int) this.contentLong );
		}

		public boolean get_tradeable()
		{
			return ItemDatabase.isTradeable( (int) this.contentLong );
		}

		public boolean get_discardable()
		{
			return ItemDatabase.isDiscardable( (int) this.contentLong );
		}

		public boolean get_combat()
		{
			return ItemDatabase.getAttribute( (int) this.contentLong, ItemDatabase.ATTR_COMBAT | ItemDatabase.ATTR_COMBAT_REUSABLE );
		}

		public boolean get_combat_reusable()
		{
			return ItemDatabase.getAttribute( (int) this.contentLong, ItemDatabase.ATTR_COMBAT_REUSABLE );
		}

		public boolean get_usable()
		{
			return ItemDatabase.isUsable( (int) this.contentLong );
		}

		public boolean get_reusable()
		{
			int id = (int) this.contentLong;
			return ItemDatabase.getConsumptionType( id ) == KoLConstants.INFINITE_USES || ItemDatabase.getAttribute( id, ItemDatabase.ATTR_REUSABLE );
		}

		public boolean get_multi()
		{
			return ItemDatabase.isMultiUsable( (int) this.contentLong );
		}

		public boolean get_fancy()
		{
			return ItemDatabase.isFancyItem( (int) this.contentLong );
		}

		public boolean get_candy()
		{
			return ItemDatabase.isCandyItem( (int) this.contentLong );
		}

		public String get_candy_type()
		{
			return CandyDatabase.getCandyType( (int) this.contentLong );
		}

		public boolean get_chocolate()
		{
			return ItemDatabase.isChocolateItem( (int) this.contentLong );
		}

		public CoinmasterData get_seller()
		{
			return CoinmasterRegistry.findSeller( (int) this.contentLong );
		}

		public CoinmasterData get_buyer()
		{
			return CoinmasterRegistry.findBuyer( (int) this.contentLong );
		}

		public int get_name_length()
		{
			return ItemDatabase.getNameLength( (int) this.contentLong );
		}

		public Value get_noob_skill()
		{
			return DataTypes.makeSkillValue( ItemDatabase.getNoobSkillId( (int) this.contentLong ), true );
		}
	}

	public static class FamiliarProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "hatchling", DataTypes.ITEM_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "name", DataTypes.STRING_TYPE )
			.add( "charges", DataTypes.INT_TYPE )
			.add( "drop_name", DataTypes.STRING_TYPE )
			.add( "drop_item", DataTypes.ITEM_TYPE )
			.add( "drops_today", DataTypes.INT_TYPE )
			.add( "drops_limit", DataTypes.INT_TYPE )
			.add( "fights_today", DataTypes.INT_TYPE )
			.add( "fights_limit", DataTypes.INT_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "physical_damage", DataTypes.BOOLEAN_TYPE )
			.add( "elemental_damage", DataTypes.BOOLEAN_TYPE )
			.add( "block", DataTypes.BOOLEAN_TYPE )
			.add( "delevel", DataTypes.BOOLEAN_TYPE )
			.add( "hp_during_combat", DataTypes.BOOLEAN_TYPE )
			.add( "mp_during_combat", DataTypes.BOOLEAN_TYPE )
			.add( "other_action_during_combat", DataTypes.BOOLEAN_TYPE )
			.add( "hp_after_combat", DataTypes.BOOLEAN_TYPE )
			.add( "mp_after_combat", DataTypes.BOOLEAN_TYPE )
			.add( "other_action_after_combat", DataTypes.BOOLEAN_TYPE )
			.add( "passive", DataTypes.BOOLEAN_TYPE )
			.add( "underwater", DataTypes.BOOLEAN_TYPE )
			.add( "variable", DataTypes.BOOLEAN_TYPE )
			.add( "attributes", DataTypes.STRING_TYPE )
			.add( "poke_level", DataTypes.INT_TYPE )
			.add( "poke_level_2_power", DataTypes.INT_TYPE )
			.add( "poke_level_2_hp", DataTypes.INT_TYPE )
			.add( "poke_level_3_power", DataTypes.INT_TYPE )
			.add( "poke_level_3_hp", DataTypes.INT_TYPE )
			.add( "poke_level_4_power", DataTypes.INT_TYPE )
			.add( "poke_level_4_hp", DataTypes.INT_TYPE )
			.add( "poke_move_1", DataTypes.STRING_TYPE )
			.add( "poke_move_2", DataTypes.STRING_TYPE )
			.add( "poke_move_3", DataTypes.STRING_TYPE )
			.add( "poke_attribute", DataTypes.STRING_TYPE )
			.finish( "familiar proxy" );

		public FamiliarProxy( Value obj )
		{
			super( _type, obj );
		}

		public Value get_hatchling()
		{
			return DataTypes.makeItemValue( FamiliarDatabase.getFamiliarLarva( (int) this.contentLong ), true );
		}

		public String get_image()
		{
			return FamiliarDatabase.getFamiliarImageLocation( (int) this.contentLong );
		}

		public String get_name()
		{
			FamiliarData fam = KoLCharacter.findFamiliar( this.contentString );
			return fam == null ? "" : fam.getName();
		}

		public int get_charges()
		{
			FamiliarData fam = KoLCharacter.findFamiliar( this.contentString );
			return fam == null ? 0 : fam.getCharges();
		}

		public String get_drop_name()
		{
			String dropName = FamiliarData.dropName( (int) this.contentLong );
			return dropName == null ? "" : dropName;
		}

		public Value get_drop_item()
		{
			AdventureResult item = FamiliarData.dropItem( (int) this.contentLong );
			return DataTypes.makeItemValue( item == null ? -1 : item.getItemId(), true );
		}

		public int get_drops_today()
		{
			return FamiliarData.dropsToday( (int) this.contentLong );
		}

		public int get_drops_limit()
		{
			return FamiliarData.dropDailyCap( (int) this.contentLong );
		}

		public int get_fights_today()
		{
			return FamiliarData.fightsToday( (int) this.contentLong );
		}

		public int get_fights_limit()
		{
			return FamiliarData.fightDailyCap( (int) this.contentLong );
		}

		public boolean get_combat()
		{
			return FamiliarDatabase.isCombatType( (int) this.contentLong );
		}

		public boolean get_physical_damage()
		{
			return FamiliarDatabase.isCombat0Type( (int) this.contentLong );
		}

		public boolean get_elemental_damage()
		{
			return FamiliarDatabase.isCombat1Type( (int) this.contentLong );
		}

		public boolean get_block()
		{
			return FamiliarDatabase.isBlockType( (int) this.contentLong );
		}

		public boolean get_delevel()
		{
			return FamiliarDatabase.isDelevelType( (int) this.contentLong );
		}

		public boolean get_hp_during_combat()
		{
			return FamiliarDatabase.isHp0Type( (int) this.contentLong );
		}

		public boolean get_mp_during_combat()
		{
			return FamiliarDatabase.isMp0Type( (int) this.contentLong );
		}

		public boolean get_other_action_during_combat()
		{
			return FamiliarDatabase.isOther0Type( (int) this.contentLong );
		}

		public boolean get_hp_after_combat()
		{
			return FamiliarDatabase.isHp1Type( (int) this.contentLong );
		}

		public boolean get_mp_after_combat()
		{
			return FamiliarDatabase.isMp1Type( (int) this.contentLong );
		}

		public boolean get_other_action_after_combat()
		{
			return FamiliarDatabase.isOther1Type( (int) this.contentLong );
		}

		public boolean get_passive()
		{
			return FamiliarDatabase.isPassiveType( (int) this.contentLong );
		}

		public boolean get_underwater()
		{
			return FamiliarDatabase.isUnderwaterType( (int) this.contentLong );
		}

		public boolean get_variable()
		{
			return FamiliarDatabase.isVariableType( (int) this.contentLong );
		}

		public String get_attributes()
		{
			List<String> attrs = FamiliarDatabase.getFamiliarAttributes( (int) this.contentLong );
			if ( attrs == null )
			{
				return "";
			}
			StringBuilder builder = new StringBuilder();
			for ( String attr : attrs )
			{
				if ( builder.length() != 0 )
				{
					builder.append( "; " );
				}
				builder.append( attr );
			}
			return builder.toString();
		}

		public int get_poke_level()
		{
			FamiliarData fam = KoLCharacter.findFamiliar( this.contentString );
			return fam == null ? 0 : fam.getPokeLevel();
		}

		public int get_poke_level_2_power()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? 0 : data.getPower2();
		}

		public int get_poke_level_2_hp()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? 0 : data.getHP2();
		}

		public int get_poke_level_3_power()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? 0 : data.getPower3();
		}

		public int get_poke_level_3_hp()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? 0 : data.getHP3();
		}

		public int get_poke_level_4_power()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? 0 : data.getPower4();
		}

		public int get_poke_level_4_hp()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? 0 : data.getHP4();
		}

		public String get_poke_move_1()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? "" : data.getMove1();
		}

		public String get_poke_move_2()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? "" : data.getMove2();
		}

		public String get_poke_move_3()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? "" : data.getMove3();
		}

		public String get_poke_attribute()
		{
			PokefamData data = FamiliarDatabase.getPokeDataById( (int) this.contentLong );
			return data == null ? "" : data.getAttribute();
		}
	}

	public static class BountyProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "plural", DataTypes.STRING_TYPE )
			.add( "type", DataTypes.STRING_TYPE )
			.add( "kol_internal_type", DataTypes.STRING_TYPE )
			.add( "number", DataTypes.INT_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "monster", DataTypes.MONSTER_TYPE )
			.add( "location", DataTypes.LOCATION_TYPE )
			.finish( "bounty proxy" );

		public BountyProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_plural()
		{
			String plural = BountyDatabase.getPlural( this.contentString );
			return plural == null ? "" : plural;
		}

		public String get_type()
		{
			String type = BountyDatabase.getType( this.contentString );
			return type == null ? "" : type;
		}

		public String get_kol_internal_type()
		{
			String type = BountyDatabase.getType( this.contentString );
			return type == null ? "" :
				type.equals( "easy" ) ? "low" :
				type.equals( "hard" ) ? "high" :
				type.equals( "special" ) ? "special" :
				null;
		}

		public int get_number()
		{
			int number = BountyDatabase.getNumber( this.contentString );
			return number;
		}

		public String get_image()
		{
			String image = BountyDatabase.getImage( this.contentString );
			return image == null ? "" : image;
		}

		public Value get_monster()
		{
			String monster = BountyDatabase.getMonster( this.contentString );
			return DataTypes.parseMonsterValue( monster == null ? "" : monster, true );
		}

		public Value get_location()
		{
			String location = BountyDatabase.getLocation( this.contentString );
			return DataTypes.parseLocationValue( location == null ? "" : location, true );
		}
	}

	public static class ThrallProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "id", DataTypes.INT_TYPE )
			.add( "name", DataTypes.STRING_TYPE )
			.add( "level", DataTypes.INT_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "tinyimage", DataTypes.STRING_TYPE )
			.add( "skill", DataTypes.SKILL_TYPE )
			.add( "current_modifiers", DataTypes.STRING_TYPE )
			.finish( "thrall proxy" );

		public ThrallProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_id()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? 0 : PastaThrallData.dataToId( data );
		}

		public String get_name()
		{
			PastaThrallData thrall = KoLCharacter.findPastaThrall( this.contentString );
			return thrall == null ? "" : thrall.getName();
		}

		public int get_level()
		{
			PastaThrallData thrall = KoLCharacter.findPastaThrall( this.contentString );
			return thrall == null ? 0 : thrall.getLevel();
		}

		public String get_image()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? "" : PastaThrallData.dataToImage( data );
		}

		public String get_tinyimage()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? "" : PastaThrallData.dataToTinyImage( data );
		}

		public Value get_skill()
		{
			Object[] data = (Object[]) this.content;
			return DataTypes.makeSkillValue( data == null ? 0 : PastaThrallData.dataToSkillId( data ), true );
		}

		public String get_current_modifiers()
		{
			PastaThrallData thrall = KoLCharacter.findPastaThrall( this.contentString );
			return thrall == null ? "" : thrall.getCurrentModifiers();
		}
	}

	public static class ServantProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "id", DataTypes.INT_TYPE )
			.add( "name", DataTypes.STRING_TYPE )
			.add( "level", DataTypes.INT_TYPE )
			.add( "experience", DataTypes.INT_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "level1_ability", DataTypes.STRING_TYPE )
			.add( "level7_ability", DataTypes.STRING_TYPE )
			.add( "level14_ability", DataTypes.STRING_TYPE )
			.add( "level21_ability", DataTypes.STRING_TYPE )
			.finish( "servant proxy" );

		public ServantProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_id()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? 0 : EdServantData.dataToId( data );
		}

		public String get_name()
		{
			EdServantData servant = EdServantData.findEdServant( this.contentString );
			return servant == null ? "" : servant.getName();
		}

		public int get_level()
		{
			EdServantData servant = EdServantData.findEdServant( this.contentString );
			return servant == null ? 0 : servant.getLevel();
		}

		public int get_experience()
		{
			EdServantData servant = EdServantData.findEdServant( this.contentString );
			return servant == null ? 0 : servant.getExperience();
		}

		public String get_image()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? "" : EdServantData.dataToImage( data );
		}

		public String get_level1_ability()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? "" : EdServantData.dataToLevel1Ability( data );
		}

		public String get_level7_ability()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? "" : EdServantData.dataToLevel7Ability( data );
		}

		public String get_level14_ability()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? "" : EdServantData.dataToLevel14Ability( data );
		}

		public String get_level21_ability()
		{
			Object[] data = (Object[]) this.content;
			return data == null ? "" : EdServantData.dataToLevel21Ability( data );
		}
	}

	public static class VykeaProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "id", DataTypes.INT_TYPE )
			.add( "name", DataTypes.STRING_TYPE )
			.add( "type", DataTypes.INT_TYPE )
			.add( "rune", DataTypes.ITEM_TYPE )
			.add( "level", DataTypes.INT_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "modifiers", DataTypes.STRING_TYPE )
			.add( "attack_element", DataTypes.ELEMENT_TYPE )
			.finish( "vykea proxy" );

		public VykeaProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_id()
		{
			return (int) this.contentLong;
		}

		public String get_name()
		{
			VYKEACompanionData companion = (VYKEACompanionData) this.content;
			return companion == null ? "" : companion.getName();
		}

		public String get_type()
		{
			VYKEACompanionData companion = (VYKEACompanionData) this.content;
			return companion == null ? "" : companion.typeToString();
		}

		public Value get_rune()
		{
			VYKEACompanionData companion = (VYKEACompanionData) this.content;
			return companion == null ? DataTypes.ITEM_INIT : DataTypes.makeItemValue( companion.getRune().getItemId(), true );
		}

		public int get_level()
		{
			VYKEACompanionData companion = (VYKEACompanionData) this.content;
			return companion == null ? 0 : companion.getLevel();
		}

		public String get_image()
		{
			VYKEACompanionData companion = (VYKEACompanionData) this.content;
			return companion == null ? "" : companion.getImage();
		}

		public String get_modifiers()
		{
			VYKEACompanionData companion = (VYKEACompanionData) this.content;
			return companion == null ? "" : companion.getModifiers();
		}

		public Value get_attack_element()
		{
			VYKEACompanionData companion = (VYKEACompanionData) this.content;
			return companion == null ? DataTypes.ELEMENT_INIT : DataTypes.makeElementValue( companion.getAttackElement(), true );
		}
	}

	public static class SkillProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "level", DataTypes.INT_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "traincost", DataTypes.INT_TYPE )
			.add( "class", DataTypes.CLASS_TYPE )
			.add( "libram", DataTypes.BOOLEAN_TYPE )
			.add( "passive", DataTypes.BOOLEAN_TYPE )
			.add( "buff", DataTypes.BOOLEAN_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "song", DataTypes.BOOLEAN_TYPE )
			.add( "expression", DataTypes.BOOLEAN_TYPE )
			.add( "walk", DataTypes.BOOLEAN_TYPE )
			.add( "summon", DataTypes.BOOLEAN_TYPE )
			.add( "permable", DataTypes.BOOLEAN_TYPE )
			.add( "dailylimit", DataTypes.INT_TYPE )
			.add( "timescast", DataTypes.INT_TYPE )
			.finish( "skill proxy" );

		public SkillProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_level()
		{
			return SkillDatabase.getSkillLevel( (int) this.contentLong );
		}

		public String get_image()
		{
			return SkillDatabase.getSkillImage( (int) this.contentLong );
		}

		public int get_traincost()
		{
			return SkillDatabase.getSkillPurchaseCost( (int) this.contentLong );
		}

		public Value get_class()
		{
			return DataTypes.parseClassValue(
				SkillDatabase.getSkillCategory( (int) this.contentLong ), true );
		}

		public boolean get_libram()
		{
			return SkillDatabase.isLibramSkill( (int) this.contentLong );
		}

		public boolean get_passive()
		{
			return SkillDatabase.isPassive( (int) this.contentLong );
		}

		public boolean get_buff()
		{
			return SkillDatabase.isBuff( (int) this.contentLong );
		}

		public boolean get_combat()
		{
			return SkillDatabase.isCombat( (int) this.contentLong );
		}

		public boolean get_song()
		{
			return SkillDatabase.isSong( (int) this.contentLong );
		}

		public boolean get_expression()
		{
			return SkillDatabase.isExpression( (int) this.contentLong );
		}

		public boolean get_walk()
		{
			return SkillDatabase.isWalk( (int) this.contentLong );
		}

		public boolean get_summon()
		{
			return SkillDatabase.isSummon( (int) this.contentLong );
		}

		public boolean get_permable()
		{
			return SkillDatabase.isPermable( (int) this.contentLong );
		}

		public int get_dailylimit()
		{
			return SkillDatabase.getMaxCasts( (int) this.contentLong );
		}

		public int get_timescast()
		{
			return SkillDatabase.getCasts( (int) this.contentLong );
		}
	}

	public static class EffectProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "default", DataTypes.STRING_TYPE )
			.add( "note", DataTypes.STRING_TYPE )
			.add( "all",
				new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE ) )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.add( "candy_tier", DataTypes.INT_TYPE )
			.finish( "effect proxy" );

		public EffectProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_default()
		{
			return EffectDatabase.getDefaultAction( (int) this.contentLong );
		}

		public String get_note()
		{
			return EffectDatabase.getActionNote( (int) this.contentLong );
		}

		public Value get_all()
		{
			ArrayList<Value> rv = new ArrayList<Value>();
			Iterator i = EffectDatabase.getAllActions( (int) this.contentLong );
			while ( i.hasNext() )
			{
				rv.add( new Value( (String) i.next() ) );
			}
			return new PluralValue( DataTypes.STRING_TYPE, rv );
		}

		public String get_image()
		{
			return EffectDatabase.getImage( (int) this.contentLong );
		}

		public String get_descid()
		{
			return EffectDatabase.getDescriptionId( (int) this.contentLong );
		}

		public int get_candy_tier()
		{
			return CandyDatabase.getEffectTier( (int) this.contentLong );
		}
	}

	public static class LocationProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "nocombats", DataTypes.BOOLEAN_TYPE )
			.add( "zone", DataTypes.STRING_TYPE )
			.add( "parent", DataTypes.STRING_TYPE )
			.add( "parentdesc", DataTypes.STRING_TYPE )
			.add( "environment", DataTypes.STRING_TYPE )
			.add( "bounty", DataTypes.BOUNTY_TYPE )
			.add( "combat_queue", DataTypes.STRING_TYPE )
			.add( "noncombat_queue", DataTypes.STRING_TYPE )
			.add( "turns_spent", DataTypes.INT_TYPE )
			.add( "kisses", DataTypes.INT_TYPE )
			.add( "recommended_stat", DataTypes.INT_TYPE )
			.add( "water_level", DataTypes.INT_TYPE )
			.finish( "location proxy" );

		public LocationProxy( Value obj )
		{
			super( _type, obj );
		}

		public boolean get_nocombats()
		{
			return ( (KoLAdventure) this.content ).isNonCombatsOnly();
		}

		public String get_zone()
		{
			return ( (KoLAdventure) this.content ).getZone();
		}

		public String get_parent()
		{
			return ( (KoLAdventure) this.content ).getParentZone();
		}

		public String get_parentdesc()
		{
			return ( (KoLAdventure) this.content ).getParentZoneDescription();
		}

		public String get_environment()
		{
			return ( (KoLAdventure) this.content ).getEnvironment();
		}

		public Value get_bounty()
		{
			AdventureResult bounty = AdventureDatabase.getBounty( (KoLAdventure) this.content );
			return bounty == null ?
				DataTypes.BOUNTY_INIT :
				DataTypes.parseBountyValue( bounty.getName(), true );
		}

		public String get_combat_queue()
		{
			List<?> zoneQueue = AdventureQueueDatabase.getZoneQueue( (KoLAdventure) this.content );
			if ( zoneQueue == null )
			{
				return "";
			}

			StringBuilder builder = new StringBuilder();
			for ( Object ob : zoneQueue )
			{
				if ( ob == null )
					continue;

				if ( builder.length() > 0 )
					builder.append( "; " );

				builder.append( ob.toString() );
			}

			return builder.toString();
		}

		public String get_noncombat_queue()
		{
			List<?> zoneQueue = AdventureQueueDatabase.getZoneNoncombatQueue( (KoLAdventure) this.content );
			if ( zoneQueue == null )
			{
				return "";
			}

			StringBuilder builder = new StringBuilder();
			for ( Object ob : zoneQueue )
			{
				if ( ob == null )
					continue;

				if ( builder.length() > 0 )
					builder.append( "; " );

				builder.append( ob.toString() );
			}

			return builder.toString();
		}

		public int get_turns_spent()
		{
			return AdventureSpentDatabase.getTurns( (KoLAdventure) this.content );
		}

		public int get_kisses()
		{
			return FightRequest.dreadKisses( (KoLAdventure) this.content );
		}

		public int get_recommended_stat()
		{
			return ( (KoLAdventure) this.content ).getRecommendedStat();
		}

		public int get_water_level()
		{
			return KoLCharacter.inRaincore() ? ( (KoLAdventure) this.content ).getWaterLevel() : 0;
		}
	}

	public static class MonsterProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "id", DataTypes.INT_TYPE )
			.add( "base_hp", DataTypes.INT_TYPE )
			.add( "base_attack", DataTypes.INT_TYPE )
			.add( "base_defense", DataTypes.INT_TYPE )
			.add( "raw_hp", DataTypes.INT_TYPE )
			.add( "raw_attack", DataTypes.INT_TYPE )
			.add( "raw_defense", DataTypes.INT_TYPE )
			.add( "base_initiative", DataTypes.INT_TYPE )
			.add( "raw_initiative", DataTypes.INT_TYPE )
			.add( "attack_element", DataTypes.ELEMENT_TYPE )
			.add( "defense_element", DataTypes.ELEMENT_TYPE )
			.add( "physical_resistance", DataTypes.INT_TYPE )
			.add( "min_meat", DataTypes.INT_TYPE )
			.add( "max_meat", DataTypes.INT_TYPE )
			.add( "min_sprinkles", DataTypes.INT_TYPE )
			.add( "max_sprinkles", DataTypes.INT_TYPE )
			.add( "base_mainstat_exp", DataTypes.FLOAT_TYPE )
			.add( "phylum", DataTypes.PHYLUM_TYPE )
			.add( "poison", DataTypes.EFFECT_TYPE )
			.add( "boss", DataTypes.BOOLEAN_TYPE )
			.add( "dummy", DataTypes.BOOLEAN_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "images",
				new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE ) )
			.add( "sub_types",
				new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE ) )
			.add( "random_modifiers",
				new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE ) )
			.add( "manuel_name", DataTypes.STRING_TYPE )
			.add( "wiki_name", DataTypes.STRING_TYPE )
			.add( "attributes", DataTypes.STRING_TYPE )
			.finish( "monster proxy" );

		public MonsterProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_id()
		{
			return ( (MonsterData) this.content ).getId();
		}

		public int get_base_hp()
		{
			return ( (MonsterData) this.content ).getHP();
		}

		public int get_base_attack()
		{
			return ( (MonsterData) this.content ).getAttack();
		}

		public int get_raw_hp()
		{
			return ( (MonsterData) this.content ).getRawHP();
		}

		public int get_raw_attack()
		{
			return ( (MonsterData) this.content ).getRawAttack();
		}

		public int get_raw_defense()
		{
			return ( (MonsterData) this.content ).getRawDefense();
		}

		public int get_base_defense()
		{
			return ( (MonsterData) this.content ).getDefense();
		}

		public int get_base_initiative()
		{
			return ( (MonsterData) this.content ).getInitiative();
		}

		public int get_raw_initiative()
		{
			return ( (MonsterData) this.content ).getRawInitiative();
		}

		public Value get_attack_element()
		{
			return DataTypes.parseElementValue(
				( (MonsterData) this.content ).getAttackElement().toString(),
				true );
		}

		public Value get_defense_element()
		{
			return DataTypes.parseElementValue(
				( (MonsterData) this.content ).getDefenseElement().toString(),
				true );
		}

		public int get_physical_resistance()
		{
			return ( (MonsterData) this.content ).getPhysicalResistance();
		}

		public int get_min_meat()
		{
			return ( (MonsterData) this.content ).getMinMeat();
		}

		public int get_max_meat()
		{
			return ( (MonsterData) this.content ).getMaxMeat();
		}

		public int get_min_sprinkles()
		{
			return ( (MonsterData) this.content ).getMinSprinkles();
		}

		public int get_max_sprinkles()
		{
			return ( (MonsterData) this.content ).getMaxSprinkles();
		}

		public double get_base_mainstat_exp()
		{
			return ( (MonsterData) this.content ).getExperience();
		}

		public Value get_phylum()
		{
			return DataTypes.parsePhylumValue(
				( (MonsterData) this.content ).getPhylum().toString(),
				true );
		}

		public Value get_poison()
		{
			int poisonLevel = ( (MonsterData) this.content ).getPoison();
			String poisonName = poisonLevel == Integer.MAX_VALUE ?
				"none" :
				EffectDatabase.getEffectName( EffectDatabase.POISON_ID[ poisonLevel ] );
			return DataTypes.parseEffectValue( poisonName, true );
		}

		public boolean get_boss()
		{
			return ( (MonsterData) this.content ).isBoss();
		}

		public boolean get_dummy()
		{
			return ( (MonsterData) this.content ).isDummy();
		}

		public String get_image()
		{
			return ( (MonsterData) this.content ).getImage();
		}

		public Value get_images()
		{
			ArrayList<Value> rv = new ArrayList<Value>();
			for ( String image : ( (MonsterData) this.content ).getImages() )
			{
				rv.add( new Value( image ) );
			}
			return new PluralValue( DataTypes.STRING_TYPE, rv );
		}

		public Value get_random_modifiers()
		{
			ArrayList<Value> rv = new ArrayList<Value>();
			for ( String attribute : ( (MonsterData) this.content ).getRandomModifiers() )
			{
				rv.add( new Value( attribute ) );
			}
			return new PluralValue( DataTypes.STRING_TYPE, rv );
		}

		public Value get_sub_types()
		{
			ArrayList<Value> rv = new ArrayList<Value>();
			for ( String attribute : ( (MonsterData) this.content ).getSubTypes() )
			{
				rv.add( new Value( attribute ) );
			}
			return new PluralValue( DataTypes.STRING_TYPE, rv );
		}

		public String get_manuel_name()
		{
			return ( (MonsterData) this.content ).getManuelName();
		}

		public String get_wiki_name()
		{
			return ( (MonsterData) this.content ).getWikiName();
		}

		public String get_attributes()
		{
			return ( (MonsterData) this.content ).getAttributes();
		}
	}

	public static class CoinmasterProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "token", DataTypes.STRING_TYPE )
			.add( "item", DataTypes.ITEM_TYPE )
			.add( "property", DataTypes.STRING_TYPE )
			.add( "available_tokens", DataTypes.INT_TYPE )
			.add( "buys", DataTypes.BOOLEAN_TYPE )
			.add( "sells", DataTypes.BOOLEAN_TYPE )
			.add( "nickname", DataTypes.STRING_TYPE )
			.finish( "coinmaster proxy" );

		public CoinmasterProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_token()
		{
			return ( (CoinmasterData) this.content ).getToken();
		}

		public Value get_item()
		{
			CoinmasterData data = ( (CoinmasterData) this.content );
			AdventureResult item = data.getItem();
			return item == null ?
				DataTypes.ITEM_INIT :
				DataTypes.makeItemValue( item.getItemId(), true );
		}

		public String get_property()
		{
			return ( (CoinmasterData) this.content ).getProperty();
		}

		public int get_available_tokens()
		{
			return ( (CoinmasterData) this.content ).availableTokens();
		}

		public boolean get_buys()
		{
			return ( (CoinmasterData) this.content ).getSellAction() != null;
		}

		public boolean get_sells()
		{
			return ( (CoinmasterData) this.content ).getBuyAction() != null;
		}

		public String get_nickname()
		{
			return ( (CoinmasterData) this.content ).getNickname();
		}
	}

	public static class ElementProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "image", DataTypes.STRING_TYPE )
			.finish( "element proxy" );

		public ElementProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_image()
		{
			switch ( (Element) this.content )
			{
			case NONE:
				return "circle.gif";
			case COLD:
				return "snowflake.gif";
			case HOT:
				return "fire.gif";
			case SLEAZE:
				return "wink.gif";
			case SPOOKY:
				return "skull.gif";
			case STENCH:
				return "stench.gif";
			// No image for Slime or Supercold in Manuel
			case SLIME:
				return "circle.gif";
			case SUPERCOLD:
				return "circle.gif";
			}
			return "";
		}
	}

	public static class PhylumProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "image", DataTypes.STRING_TYPE )
			.finish( "phylum proxy" );

		public PhylumProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_image()
		{
			switch ( (Phylum) this.content )
			{
			case NONE:
				return "";
			case BEAST:
				return "beastflavor.gif";
			case BUG:
				return "stinkbug.gif";
			case CONSTELLATION:
				return "star.gif";
			case CONSTRUCT:
				return "sprocket.gif";
			case DEMON:
				return "demonflavor.gif";
			case DUDE:
				return "happy.gif";
			case ELEMENTAL:
				return "rrainbow.gif";
			case ELF:
				return "elfflavor.gif";
			case FISH:
				return "fish.gif";
			case GOBLIN:
				return "goblinflavor.gif";
			case HIPPY:
				return "hippyflavor.gif";
			case HOBO:
				return "hoboflavor.gif";
			case HUMANOID:
				return "statue.gif";
			case HORROR:
				return "skull.gif";
			case MER_KIN:
				return "merkinflavor.gif";
			case ORC:
				return "frattyflavor.gif";
			case PENGUIN:
				return "bowtie.gif";
			case PIRATE:
				return "pirateflavor.gif";
			case PLANT:
				return "leafflavor.gif";
			case SLIME:
				return "sebashield.gif";
			case UNDEAD:
				return "spookyflavor.gif";
			case WEIRD:
				return "weirdflavor.gif";
			}
			return "";
		}
	}
}
