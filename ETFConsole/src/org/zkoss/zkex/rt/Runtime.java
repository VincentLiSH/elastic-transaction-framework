package org.zkoss.zkex.rt;

/*** Eclipse Class Decompiler plugin, copyright (c) 2012 Chao Chen (cnfree2000@hotmail.com) ***/

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zkex.license.CipherParam;
import org.zkoss.zkex.license.KeyStoreParam;
import org.zkoss.zkex.license.LicenseParam;
import org.zkoss.zkex.util.ObfuscatedString;

public final class Runtime {
	public static final String				COMPANY_NAME				= new ObfuscatedString(new long[] {
			-7340139527016707886L, -5203243759892677212L, 8714822623115369524L }).toString();
	public static final String				COMPANY_ADDRESS				= new ObfuscatedString(new long[] {
			-8691848786421899489L, -4370996558863340632L, -2709933490946981238L }).toString();
	public static final String				COMPANY_ZIPCODE				= new ObfuscatedString(new long[] {
			-4934501068656857753L, -6913828373145765012L, 1063193634233753528L }).toString();
	public static final String				COUNTRY						= new ObfuscatedString(new long[] {
			-4504969373906269801L, 5248137891553083335L				}).toString();
	public static final String				PROJECT_NAME				= new ObfuscatedString(new long[] {
			830623621279886032L, 459360910074040759L, 4178520520701877821L }).toString();
	public static final String				PRODUCT_NAME				= new ObfuscatedString(new long[] {
			5504882338648710617L, -4761731334749763195L, 996375918662338065L }).toString();
	public static final String				PACKAGE						= new ObfuscatedString(new long[] {
			-8439029564924938530L, -4878278112849633009L				}).toString();
	public static final String				VERSION						= new ObfuscatedString(new long[] {
			-4847689528984584834L, 2493253216426408014L				}).toString();
	public static final String				ISSUE_DATE					= new ObfuscatedString(new long[] {
			-4228764154858292882L, -6898004159031332466L, 6328666951570048917L }).toString();
	public static final String				EXPIRY_DATE					= new ObfuscatedString(new long[] {
			-7233890858958970371L, -3423973165832030856L, -5810612950970282077L }).toString();
	public static final String				TERM						= new ObfuscatedString(new long[] {
			-6725301182108235475L, -8691110408124621856L				}).toString();
	public static final String				VERIFICATION_NUMBER			= new ObfuscatedString(new long[] {
			3823288740853721680L, -6436340937747658512L, 891079956768101415L, 751513662528431611L }).toString();
	public static final String				INFORMATION					= new ObfuscatedString(new long[] {
			378870925371295609L, 1418863983102047429L, -5017007170548372422L }).toString();
	public static final String				KEY_SIGNATURE				= new ObfuscatedString(new long[] {
			-2573177027008659676L, 5066716785755217927L, 5769746383701090690L }).toString();
	public static final String				CHECK_PERIOD				= new ObfuscatedString(new long[] {
			-2439022525501632135L, 6139476070014855270L, -297657911147084449L }).toString();
	public static final String				LICENSE_DIRECTORY_PROPERTY	= new ObfuscatedString(new long[] {
			6388899238000244134L, -5180024805664342415L, -367736596541156579L, -5744200612985226406L,
			-3419809629710828214L, -3830138366774401580L				}).toString();
	public static final String				LICENSE_VERSION				= new ObfuscatedString(new long[] {
			-7080462743270045357L, 6928867389785115158L, -154565539896996742L }).toString();
	public static final String				WARNING_EXPIRY				= new ObfuscatedString(new long[] {
			-2088056424898980973L, -3616911578495445651L, -8353968737700076168L }).toString();
	public static final String				WARNING_PACKAGE				= new ObfuscatedString(new long[] {
			7436618834759965309L, 8220698497085578148L, -6394078374620879850L }).toString();
	public static final String				WARNING_VERSION				= new ObfuscatedString(new long[] {
			7417971821667979026L, 7464186339852802771L, 7986314911006223431L }).toString();
	public static final String				WARNING_COUNT				= new ObfuscatedString(new long[] {
			-1510608780643214737L, 6313704540210276937L, 4115504365890483558L }).toString();
	public static final String				WARNING_NUMBER				= new ObfuscatedString(new long[] {
			8367990676393660796L, -7163797910637480555L, -8349581027556623805L }).toString();
	public static final String				WARNING_EVALUATION			= new ObfuscatedString(new long[] {
			-2937671592507492851L, -3102268496324167801L, 5919586344461932935L, 5212380637393923732L,
			-9183933033647938375L, -5639401357834602762L, 1900792064421269769L, 9104764753224514299L,
			-366291431641907250L, -4981951446456859208L, 4621325894282509391L, 4597809274251359252L,
			320404476883389261L, 4834059445850194399L, 8189661530836744639L, 3321360458823117467L,
			-5649982894045768422L, 6824790053669282079L, 2913592521239689585L, 792727853396105105L,
			-5806058457130989498L, -9213406388190282821L, 6249188679012732153L, -5765366963941016085L,
			6083980417978954274L, 5349913069684837185L, -8468607902170715087L, -231630823878680548L,
			-7028398969894246595L, -4717713309948778529L, -6063904821125494506L, -3266075547796484583L,
			-3094008223659612898L, 5979310533272008024L, -9081613057476004509L, 2696821719504131438L,
			8070012780166355462L, -7898308780045792598L, 7667082605530285313L, -8372608728474611036L,
			-4196725015623600895L										}).toString();
	private static boolean					_ck;
	private static final long[]				KEY_SIG_CONST				= { -2094697673918916784L,
			9043705620287016241L, 4353964247040842695L					};
	private static final String				PUB_STORE					= new ObfuscatedString(new long[] {
			5347140503694695672L, -2050078472222786559L, -1090939667477166182L, -1614887116429751062L }).toString();
	private static final String				SUBJECT						= new ObfuscatedString(new long[] {
			-1712853941495412010L, 7139314103872750678L, 1708647545312167373L }).toString();
	private static final String				KEY_NODE					= new ObfuscatedString(new long[] {
			-1178633844166344050L, 942064108562773571L, 5467556404179583502L }).toString();
	private static final String				ALIAS						= new ObfuscatedString(new long[] {
			6186746913238977211L, 1118071448919910242L, 3268266759092267502L, 8415914926935764836L,
			8050900615525030165L										}).toString();
	private static final String				STORE_PASS					= new ObfuscatedString(new long[] {
			8215178821005386738L, 3606545547696508380L, 8105569695056259631L, 8978067179702718063L,
			8980164237678180832L										}).toString();
	private static volatile long			_uptime						= -1L;
	private static final String				SCHEDULE_DISABLED			= new ObfuscatedString(new long[] {
			5671306788677542365L, -5885525372458450988L, -4407396626133081282L, -1537455835363693899L,
			-6031102665156057524L, -1478170211770868218L				}).toString();
	private static final String				UPTIME_INFO					= new ObfuscatedString(new long[] {
			-140629977948033438L, 8411039821423448906L, -3474650949739896324L, 2772224587032503194L }).toString();
	private static final String				UPTIME_EXP					= new ObfuscatedString(new long[] {
			3013278860660930162L, 4301471526854951301L, -6747772497216156675L, 5645720567963480327L,
			6758055736309218282L, -1654731230014362443L, 274486467337341086L, 7040862789364547387L,
			7601088371900655428L, -2636893431972966937L, -952327415151053477L, 1248377226099978011L,
			-524117267375531873L, -2053861175643715280L, -740110635865205725L, -2536839436544335127L,
			6086939566297226655L, -5237082058524871272L, 1303639338833085497L, -6089458893752999207L,
			-3095527103706203994L, 3892762200748726282L, -9216442073544683168L, 2684244160057465715L,
			-2757286429096684263L, 3994511167925782802L, -6001272987401509012L, -8806371164062622569L,
			-8363562597909226838L, -6927551160336525363L, -4211670988672996905L, 5763267769455554081L,
			-117125249251658674L, 3683653926807866520L, -7901206021548053328L, 6018772853677880081L,
			-1465864270908134531L, -6931709528463852872L, 4789754509887854166L, 890869380472621700L,
			-4164923529230262421L, 2107536733021848521L, 1569761813715635289L, -4115748546260801472L }).toString();
	private static KeyStoreParam			_keystoreParam				= new KeyStoreParam() {
																			////@Override
																			@Override
																			public InputStream getStream()
																					throws IOException {
																				InputStream localInputStream = Thread
																						.currentThread()
																						.getContextClassLoader()
																						.getResourceAsStream(
																								Runtime.PUB_STORE);
																				if (localInputStream == null)
																					throw new FileNotFoundException(
																							Runtime.PUB_STORE);
																				return localInputStream;
																			}

																			////@Override
																			@Override
																			public String getAlias() {
																				return Runtime.ALIAS;
																			}

																			//@Override
																			@Override
																			public String getStorePwd() {
																				return Runtime.STORE_PASS;
																			}

																			//@Override
																			@Override
																			public String getKeyPwd() {
																				return null;
																			}
																		};
	private static CipherParam				_cipherParam				= new CipherParam() {
																			//@Override
																			@Override
																			public String getKeyPwd() {
																				return new ObfuscatedString(new long[] {
			-9017617134232705315L, -3067316756544620689L, -7174741455541659722L, 9223059116147577819L,
			-7389013047307896124L												}).toString();
																			}
																		};
	private static LicenseParam				_licenseParam				= new LicenseParam() {
																			//@Override
																			@Override
																			public String getSubject() {
																				return Runtime.SUBJECT;
																			}

																			//@Override
																			@Override
																			public Preferences getPreferences() {
																				return null;
																			}

																			//@Override
																			@Override
																			public KeyStoreParam getKeyStoreParam() {
																				return Runtime._keystoreParam;
																			}

																			//@Override
																			@Override
																			public CipherParam getCipherParam() {
																				return Runtime._cipherParam;
																			}
																		};
	private static RuntimeLicenseManager	_licManager					= RuntimeLicenseManager
																				.getInstance(_licenseParam);

	public static final Object init(Object paramObject) {
		//		if ((_uptime > 0L) && (_uptime < new Date().getTime())) {
		//			String str = (WebApps.getFeature("ee")) ? "EE" : "PE";
		//			str = UPTIME_EXP.replaceAll("\\{0\\}", str);
		//			Logger localLogger = LoggerFactory.getLogger("global");
		//			if (localLogger.isErrorEnabled())
		//				localLogger.error(str);
		//			else
		//				System.err.println(str);
		//			Execution localExecution = Executions.getCurrent();
		//			if (localExecution != null)
		//				localExecution.addAuResponse(UPTIME_EXP, new AuAlert(str, "ZK Eval Notice", "z-msgbox z-uptime"));
		//		}
		//		System.out.println("Object init!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		return new RtInfo() {
			//@Override
			@Override
			public void verify(Execution paramExecution) {
			}

			//@Override
			@Override
			public void verify(Session paramSession) {
			}
		};
	}

	public static final Object init(Object paramObject1, Object paramObject2) {
		return new RtInfo() {
			//@Override
			@Override
			public void verify(Execution paramExecution) {
			}

			//@Override
			@Override
			public void verify(Session paramSession) {
			}
		};
	}

	//	private static final String read(String paramString) {
	//		try {
	//			InputStream localInputStream = Runtime.class.getResourceAsStream("/metainfo/zk/" + paramString);
	//			if (localInputStream != null)
	//				return new String(Files.readAll(localInputStream));
	//		} catch (Throwable localThrowable) {
	//		}
	//		return null;
	//	}

	public static final boolean init(WebApp paramWebApp, boolean paramBoolean) {
		//		int i = 1;
		//		if ((paramBoolean) && (!(_ck))) {
		//			_ck = true;
		//			i = (((WebApps.getFeature("ee")) && ("ZK EE".equals(read("ee")))) || ("ZK PE".equals(read("pe")))) ? 1 : 0;
		//			String str = Library.getProperty(LICENSE_DIRECTORY_PROPERTY);
		//			boolean bool = false;
		//			if (str != null) {
		//				bool = _licManager.install(str);
		//			} else {
		//				URL localURL = Runtime.class.getResource("/metainfo/zk/license/");
		//				if (localURL != null)
		//					bool = _licManager.install(localURL.getPath());
		//			}
		//			if (i != 0)
		//				paramWebApp = null;
		//			if (bool) {
		//				_licManager.setWapp(paramWebApp);
		//				if (!("true".equals(Library.getProperty(SCHEDULE_DISABLED, "false"))))
		//					_licManager.startScheduler();
		//				return true;
		//			}
		//			if (paramWebApp != null) {
		//				paramWebApp.setAttribute("org.zkoss.zk.ui.notice", " Evaluation Only");
		//				_uptime = new Date().getTime() + 3600000 * 72 / Calendar.getInstance().get(7);
		//			}
		//		}
		//		return i;
		//		System.out.println("boolean init!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		return true;
	}
}