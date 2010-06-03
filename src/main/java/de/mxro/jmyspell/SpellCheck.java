package de.mxro.jmyspell;


import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.text.JTextComponent;

import org.dts.spell.SpellChecker;
import org.dts.spell.dictionary.OpenOfficeSpellDictionary;
import org.dts.spell.dictionary.SpellDictionary;
import org.dts.spell.swing.JTextComponentSpellChecker;

import de.mxro.filesystem.File;
import de.mxro.filesystem.Folder;
import de.mxro.utils.URIImpl;
import de.mxro.utils.application.BackgroundProcess;
import de.mxro.utils.background.Activity;
import de.mxro.utils.log.UserError;

/**
 * Facade for JMySpell
 * @author mer
 *
 */
public class SpellCheck {
	private final Folder dictionaryFolder;
	
	private final BackgroundProcess backgroundProcess=new BackgroundProcess();
	
	public final BackgroundProcess getBackgroundProcess() {
		return this.backgroundProcess;
	}
	
	private transient final HashMap<JTextComponent, JTextComponentSpellChecker> spellCheckersSwing;
	private Map<Locale, SpellChecker> spellCheckers;
	
	
	public SpellChecker getSpellChecker(Locale locale) {
		if (!this.spellCheckers.containsKey(locale)) {
			de.mxro.utils.log.UserError.singelton.log("SpellCheck: load dictionary: "+URIImpl.create(locale.getCountry()+"_"+locale.getLanguage()+".zip"), UserError.Priority.INFORMATION);
			final File file = this.dictionaryFolder.getFile(URIImpl.create(locale.getCountry()+"_"+locale.getLanguage()+".zip"));
			if (file == null)
				return null;
			
			SpellDictionary dict;
			try {
				dict = new OpenOfficeSpellDictionary(new ZipFile(file.makeLocal()));
			} catch (final ZipException e) {
				UserError.singelton.log(e);
				return null;
			} catch (final IOException e) {
				UserError.singelton.log(e);
				return null;
			}
			
			final SpellChecker checker = new SpellChecker(dict);
			
			this.spellCheckers.put(locale, checker);
			return checker;
		}
		
		return this.spellCheckers.get(locale);
	}
	
	public SpellCheck(final Folder dictionaryFolder) {
		super();
		this.dictionaryFolder = dictionaryFolder;
		this.spellCheckers = new HashMap<Locale, SpellChecker>();
		this.spellCheckersSwing = new HashMap<JTextComponent, JTextComponentSpellChecker>();
	}
	
	private class StartRealtimeSpellCheckActivity implements Activity {
		private final JTextComponent textComponent;
		private final Locale locale;
		
		public void run() {
			Locale searchFor;
			if (this.locale.getCountry().equals("DE")) {
				searchFor = new Locale("DE", "de");
			} else {
				searchFor = new Locale("US", "en");
			}
			final SpellChecker spellChecker = SpellCheck.this.getSpellChecker(searchFor);
			if (spellChecker == null)
				return;
			
			
			final JTextComponentSpellChecker textSpellChecker = new JTextComponentSpellChecker(spellChecker) ; 
			textSpellChecker.startRealtimeMarkErrors(this.textComponent);
			SpellCheck.this.spellCheckersSwing.put(this.textComponent, textSpellChecker);
			//if (this.textComponent instanceof TextItemPanel.EditorPane) {
			//	((TextItemPanel.EditorPane) this.textComponent).textSpellChecker = textSpellChecker;
			//}
			
		}
		
		public StartRealtimeSpellCheckActivity(final JTextComponent textComponent, final Locale locale) {
			super();
			this.textComponent = textComponent;
			this.locale = locale;
		}
		
		
	}
	
	private class StopRealtimeSpellCheckActivity implements Activity {
		private final JTextComponent textComponent;
		
		
		public void run() {
			final Object checker = SpellCheck.this.spellCheckersSwing.get(this.textComponent);
			if (checker != null) {
				//de.mxro.UserError.singelton.log("LinnkApplication: Stop spell check", UserError.Priority.LOW);
				final JTextComponentSpellChecker textSpellChecker = (JTextComponentSpellChecker) checker;
				textSpellChecker.stopRealtimeMarkErrors();
				SpellCheck.this.spellCheckersSwing.remove(this.textComponent);
			}
			
			
		}
		
		public StopRealtimeSpellCheckActivity(final JTextComponent textComponent) {
			super();
			this.textComponent = textComponent;
		}
		
		
	}
	
	public void startRealtimeSpellCheck(JTextComponent textComponent, Locale locale) {
		this.getBackgroundProcess().addActivity(new StartRealtimeSpellCheckActivity(textComponent, locale));
	}
	
	
	
	public void stopRealtimeSpellCheck(JTextComponent textComponent) {
		this.getBackgroundProcess().addActivity(new StopRealtimeSpellCheckActivity(textComponent));
	}
	
}
