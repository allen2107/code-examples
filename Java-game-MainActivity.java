package developz.quizoctave;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appodeal.ads.Appodeal;
import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.IabResult;
import com.example.android.trivialdrivesample.util.Inventory;
import com.example.android.trivialdrivesample.util.Purchase;

import java.util.Random;


public class MainActivity extends AppCompatActivity {
    private static final String BASE64_PUBLIC_KEY = "***";
    static final String SKU_ADS_DISABLE = "disabledads";
    IabHelper mHelper;
    private static final String TAG = "MyCat";
    protected int CurrentScore = 0;
    int Lives = 5;
    static KeyBoard MyKeyBoard;
    static PointsBar MyBar;
    int minoctave = 3;
    int maxoctave = 4;
    static final int RC_REQUEST = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "OnCreateLOG");

        // грузим настройки
        PreferencesHelper.loadSettings(getApplicationContext());
        SharedPref.getInstance(this).writeIntPreference(SharedPref.APP_PREFERENCES_ADFLAG, 0);

        MyKeyBoard = new KeyBoard();
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //Редактируем размерные параметры кнопок
        MyKeyBoard.CreateOfButtons();
        MyKeyBoard.createAllSP();
        //Создаем бар с жизнями и очками
        MyBar = new PointsBar();
        MyBar.BarCreation();
        MyKeyBoard.FillspOfNotes();

        //Начинаем новую игру
        MyKeyBoard.octavenumber = randomOctave();
        MyKeyBoard.notenumber = randomNote();
        MyKeyBoard.MainMusicButton(MyKeyBoard.octavenumber, MyKeyBoard.notenumber);
        billingInit();
    }

    public void adShow(boolean show) {
        if (show && SharedPref.getInstance(this).getIntPreference(SharedPref.APP_PREFERENCES_ADFLAG, 0)==3) {
            String appKey = "***";
            Appodeal.initialize(this, appKey, Appodeal.INTERSTITIAL | Appodeal.BANNER);
            Appodeal.show(this, Appodeal.BANNER_CENTER);
        } else {
            Appodeal.hide(this, Appodeal.BANNER_CENTER);
        }
    }

    private void billingInit() {
        mHelper = new IabHelper(this, BASE64_PUBLIC_KEY);

        // дебагинг
        mHelper.enableDebugLogging(false);

        // инициализируем; запрос асинхронен
        // будет вызван, когда инициализация завершится
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    return;
                }

                // проверяем уже купленное
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // Слушатель для востановителя покупок.
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        private static final String TAG = "QueryInventoryFinished";

        public void onQueryInventoryFinished(IabResult result,
                                             Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");
            if (result.isFailure()) {
                Log.d(TAG, "Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

			/*
			 * Проверяются покупки.
			 */

            Purchase purchase = inventory.getPurchase(SKU_ADS_DISABLE);
            PreferencesHelper.savePurchase(getApplicationContext(),
                    PreferencesHelper.Purchase.DISABLE_ADS, purchase != null
                            && verifyDeveloperPayload(purchase));
            //запуск программы в зависимости от состояния покупки (true/false), запускается при false
            adShow(!PreferencesHelper.isAdsDisabled());

        }
    };

    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

    public void buy(){
        if(!PreferencesHelper.isAdsDisabled()){
            String payload = "";
            mHelper.launchPurchaseFlow(this, SKU_ADS_DISABLE, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        }
    }

    // Прокает, когда покупка завершена
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: "
                    + purchase);
            if (result.isFailure()) {
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_ADS_DISABLE)) {

                Log.d(TAG, "Purchase for disabling ads done. Congratulating user.");
                Toast.makeText(getApplicationContext(), "Purchase for disabling ads done.", Toast.LENGTH_SHORT).show();
                // сохраняем в настройках, что отключили рекламу
                PreferencesHelper.savePurchase(getApplicationContext(), PreferencesHelper.Purchase.DISABLE_ADS, true);
                // отключаем рекламу
                adShow(!PreferencesHelper.isAdsDisabled());
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHelper != null)
            mHelper.dispose();
        mHelper = null;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled");
        }
    }

	//установка режимы игры
    protected void setGameMode() {
        int gamelevel;
        gamelevel = SharedPref.getInstance(this).getIntPreference(SharedPref.APP_PREFERENCES_LEVEL, 0);
        minoctave = 3 - gamelevel;
        maxoctave = 4 + gamelevel;
    }
	
	//выбор случайной ноты в выбранной октаве
    protected int randomNote() {
        int out;
        final Random random = new Random();
        out = random.nextInt(12);
        return out;
    }

	//выбор случайной октавы
    protected int randomOctave() {
        int out;
        final Random random = new Random();
        out = random.nextInt(maxoctave - minoctave) + minoctave;
        return out;
    }

	//очки и жизни
    class PointsBar {
        ImageView HeartImage = (ImageView) findViewById(R.id.Heart);
        TextView HeartScore = (TextView) findViewById(R.id.HeartScore);
        TextView ScoreView = (TextView) findViewById(R.id.ScoreId);

        protected void BarCreation() {
            //Получаем размер экрана
            Log.i(TAG, "Получаем размер экрана");
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int DisplayHeight = size.y;

            int HeartHeight = DisplayHeight / 16;
            int HeartWidth = DisplayHeight / 12;
            Log.i(TAG, "Посчитали размер сердца");

            ScoreView.setHeight(HeartHeight);
            ScoreView.setText(String.format(getResources().getString(R.string.score), CurrentScore));
            RelativeLayout.LayoutParams ParamsForHeartScore = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            ParamsForHeartScore.setMargins(HeartWidth, 0, 0, 0);
            HeartScore.setTextSize(HeartHeight / 3);
            HeartScore.setLayoutParams(ParamsForHeartScore);
            HeartScore.setText(String.format(getResources().getString(R.string.lives), Lives));
            RelativeLayout.LayoutParams ParamsForHeart = new RelativeLayout.LayoutParams(HeartWidth, HeartHeight);
            HeartImage.setLayoutParams(ParamsForHeart);
        }
    }

	//клавиши пианино
    class KeyBoard implements View.OnTouchListener {
        ImageButton MainButton;
        ImageButton WhiteButtonDO = (ImageButton) findViewById(R.id.WhiteButtonDO);
        ImageButton WhiteButtonRE = (ImageButton) findViewById(R.id.WhiteButtonRE);
        ImageButton WhiteButtonMI = (ImageButton) findViewById(R.id.WhiteButtonMI);
        ImageButton WhiteButtonFA = (ImageButton) findViewById(R.id.WhiteButtonFA);
        ImageButton WhiteButtonSOL = (ImageButton) findViewById(R.id.WhiteButtonSOL);
        ImageButton WhiteButtonLA = (ImageButton) findViewById(R.id.WhiteButtonLA);
        ImageButton WhiteButtonSI = (ImageButton) findViewById(R.id.WhiteButtonSI);
        ImageButton BlackButtonDO = (ImageButton) findViewById(R.id.BlackButtonDO);
        ImageButton BlackButtonRE = (ImageButton) findViewById(R.id.BlackButtonRE);
        ImageButton BlackButtonFA = (ImageButton) findViewById(R.id.BlackButtonFA);
        ImageButton BlackButtonSOL = (ImageButton) findViewById(R.id.BlackButtonSOL);
        ImageButton BlackButtonLA = (ImageButton) findViewById(R.id.BlackButtonLA);
        private int numbsOfWhites = 7;
        private int numbsOfBlacks = 5;
        private int numbsOfKeys = numbsOfBlacks + numbsOfWhites;
        private int numbsOfOctaves = 7;
        //для проигрывания звука
        SoundPool mSoundPool;
        SoundPool[][] spOfNotes = new SoundPool[numbsOfOctaves][numbsOfKeys];
        //туда передается id разных звуков
        protected int mymainnote;
        protected int[][] keyNote = new int[numbsOfOctaves][numbsOfKeys];
        //номер октавы
        public int octavenumber;
        public int notenumber;
        //хандлер для создания потока обработки изображений
        Handler handler;
        //двумерный массив нот по октавам
        int[][] ArrayOfNotes = {{R.raw.piano_c1, R.raw.piano_csharp1, R.raw.piano_d1, R.raw.piano_dsharp1, R.raw.piano_e1, R.raw.piano_f1, R.raw.piano_fsharp1, R.raw.piano_g1, R.raw.piano_gsharp1, R.raw.piano_a1, R.raw.piano_asharp1, R.raw.piano_h1},
                {R.raw.piano_c2, R.raw.piano_csharp2, R.raw.piano_d2, R.raw.piano_dsharp2, R.raw.piano_e2, R.raw.piano_f2, R.raw.piano_fsharp2, R.raw.piano_g2, R.raw.piano_gsharp2, R.raw.piano_a2, R.raw.piano_asharp2, R.raw.piano_h2},
                {R.raw.piano_c3, R.raw.piano_csharp3, R.raw.piano_d3, R.raw.piano_dsharp3, R.raw.piano_e3, R.raw.piano_f3, R.raw.piano_fsharp3, R.raw.piano_g3, R.raw.piano_gsharp3, R.raw.piano_a3, R.raw.piano_asharp3, R.raw.piano_h3},
                {R.raw.piano_c4, R.raw.piano_csharp4, R.raw.piano_d4, R.raw.piano_dsharp4, R.raw.piano_e4, R.raw.piano_f4, R.raw.piano_fsharp4, R.raw.piano_g4, R.raw.piano_gsharp4, R.raw.piano_a4, R.raw.piano_asharp4, R.raw.piano_h4},
                {R.raw.piano_c5, R.raw.piano_csharp5, R.raw.piano_d5, R.raw.piano_dsharp5, R.raw.piano_e5, R.raw.piano_f5, R.raw.piano_fsharp5, R.raw.piano_g5, R.raw.piano_gsharp5, R.raw.piano_a5, R.raw.piano_asharp5, R.raw.piano_h5},
                {R.raw.piano_c6, R.raw.piano_csharp6, R.raw.piano_d6, R.raw.piano_dsharp6, R.raw.piano_e6, R.raw.piano_f6, R.raw.piano_fsharp6, R.raw.piano_g6, R.raw.piano_gsharp6, R.raw.piano_a6, R.raw.piano_asharp6, R.raw.piano_h6},
                {R.raw.piano_c7, R.raw.piano_csharp7, R.raw.piano_d7, R.raw.piano_dsharp7, R.raw.piano_e7, R.raw.piano_f7, R.raw.piano_fsharp7, R.raw.piano_g7, R.raw.piano_gsharp7, R.raw.piano_a7, R.raw.piano_asharp7, R.raw.piano_h7}};
        int[] ArrayOfIDs = {R.id.WhiteButtonDO, R.id.BlackButtonDO, R.id.WhiteButtonRE, R.id.BlackButtonRE, R.id.WhiteButtonMI, R.id.WhiteButtonFA, R.id.BlackButtonFA, R.id.WhiteButtonSOL, R.id.BlackButtonSOL, R.id.WhiteButtonLA, R.id.BlackButtonLA, R.id.WhiteButtonSI};

        //Метод для редактирования размеров кнопок
        protected void CreateOfButtons() {
            //Получаем размер экрана
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int DisplayWidth = size.x;
            int DisplayHeight = size.y;

            //Получаем размер горизонтального отступа у клавиатуры
            int piano_horizontal_margin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);

            //Вычисление ширины кнопок
            int BlackButtonWidth = (DisplayWidth - (2 * piano_horizontal_margin)) / 10;
            int WhiteButtonWidth = (DisplayWidth - (2 * piano_horizontal_margin)) / 7;
            int MainButtonSize = DisplayHeight / 4;
            Log.i(TAG, "Посчитали ширину кнопок");

            //Создание и определение объектов-кнопок

            //Главная кнопка
            MainButton = (ImageButton) findViewById(R.id.MainButton);

            LinearLayout.LayoutParams ParamsForMainButton = new LinearLayout.LayoutParams(MainButtonSize, MainButtonSize);
            MainButton.setLayoutParams(ParamsForMainButton);
            Log.i(TAG, "Дали главной кнопке размер");

            //Задаем ширину для белых кнопок
            LinearLayout.LayoutParams ParamsForWhiteButtons = new LinearLayout.LayoutParams(WhiteButtonWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            WhiteButtonDO.setLayoutParams(ParamsForWhiteButtons);
            WhiteButtonRE.setLayoutParams(ParamsForWhiteButtons);
            WhiteButtonMI.setLayoutParams(ParamsForWhiteButtons);
            WhiteButtonFA.setLayoutParams(ParamsForWhiteButtons);
            WhiteButtonSOL.setLayoutParams(ParamsForWhiteButtons);
            WhiteButtonLA.setLayoutParams(ParamsForWhiteButtons);
            WhiteButtonSI.setLayoutParams(ParamsForWhiteButtons);
            Log.i(TAG, "Дали белым кнопкам постоянную ширину");

            //Создание параметров для черных кнопок
            LinearLayout.LayoutParams ParamsForBlackButtonDO = new LinearLayout.LayoutParams(BlackButtonWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            //Вычисление отступов для черных кнопок
            int BigBlackMargin = WhiteButtonWidth - BlackButtonWidth / 2;
            int SmallBlackMargin = WhiteButtonWidth - BlackButtonWidth;
            //Применение параметров для черных кнопок
            ParamsForBlackButtonDO.setMargins(BigBlackMargin, 0, SmallBlackMargin, 0);
            BlackButtonDO.setLayoutParams(ParamsForBlackButtonDO);
            LinearLayout.LayoutParams ParamsForBlackButtonRE = new LinearLayout.LayoutParams(BlackButtonWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            BlackButtonRE.setLayoutParams(ParamsForBlackButtonRE);
            LinearLayout.LayoutParams ParamsForBlackButtonFA = new LinearLayout.LayoutParams(BlackButtonWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            ParamsForBlackButtonFA.setMargins(2 * BigBlackMargin, 0, SmallBlackMargin, 0);
            BlackButtonFA.setLayoutParams(ParamsForBlackButtonFA);
            LinearLayout.LayoutParams ParamsForBlackButtonSOL = new LinearLayout.LayoutParams(BlackButtonWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            BlackButtonSOL.setLayoutParams(ParamsForBlackButtonSOL);
            LinearLayout.LayoutParams ParamsForBlackButtonLA = new LinearLayout.LayoutParams(BlackButtonWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            ParamsForBlackButtonLA.setMargins(SmallBlackMargin, 0, BigBlackMargin, 0);
            BlackButtonLA.setLayoutParams(ParamsForBlackButtonLA);
            Log.i(TAG, "Дали размеры отступов и ширины черным кнопкам");
        }

		//загрузка звуков клавиш
        protected void FillspOfNotes() {
            Log.i(TAG, "Загружаем ноты!");
            int k = 0;
            for (int i = 0; i < numbsOfOctaves; i++) {
                for (int j = 0; j < numbsOfKeys; j++) {
                    keyNote[i][j] = spOfNotes[i][j].load(MainActivity.this, ArrayOfNotes[i][j], 1);
                    Log.i(TAG, "Загружаем ресурс номер " + Integer.toString(k));
                    k += 1;
                }
                Log.i(TAG, "Загружили октаву номер " + Integer.toString(i));
            }
            WhiteButtonDO.setOnTouchListener(this);
            BlackButtonDO.setOnTouchListener(this);
            WhiteButtonRE.setOnTouchListener(this);
            BlackButtonRE.setOnTouchListener(this);
            WhiteButtonMI.setOnTouchListener(this);
            WhiteButtonFA.setOnTouchListener(this);
            BlackButtonFA.setOnTouchListener(this);
            WhiteButtonSOL.setOnTouchListener(this);
            BlackButtonSOL.setOnTouchListener(this);
            WhiteButtonLA.setOnTouchListener(this);
            BlackButtonLA.setOnTouchListener(this);
            WhiteButtonSI.setOnTouchListener(this);
        }

		//звук главной кнопки
        protected void MainMusicButton(int i, int j) {
            mymainnote = mSoundPool.load(MainActivity.this, ArrayOfNotes[i][j], 1);
            MainButton.setOnTouchListener(this);
            Log.i(TAG, "Загрузили звук для главной кнопки");
        }

        protected void createAllSP() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                createNewSoundPool();
            } else {
                createOldSoundPool();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                createNewspOfNotes();
            } else {
                createOldspOfNotes();
            }
            Log.i(TAG, "Создали все SP");
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void createNewspOfNotes() {
            Log.i(TAG, "Начинаем createNewspOfNotes");
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            for (int i = 0; i < numbsOfOctaves; i++) {
                for (int j = 0; j < numbsOfKeys; j++) {
                    spOfNotes[i][j] = new SoundPool.Builder()
                            .setAudioAttributes(attributes)
                            .build();
                }
            }
            Log.i(TAG, "Создали массив spOfNotes");
        }

        @SuppressWarnings("deprecation")
        private void createOldspOfNotes() {
            for (int i = 0; i < numbsOfOctaves; i++) {
                for (int j = 0; j < numbsOfKeys; j++) {
                    Log.i(TAG, "i,j=" + Integer.toString(i) + ", " + Integer.toString(j));
                    spOfNotes[i][j] = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
                }
            }
            Log.i(TAG, "Создали старый массив spOfNotes");
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void createNewSoundPool() {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mSoundPool = new SoundPool.Builder()
                    .setAudioAttributes(attributes)
                    .build();
            Log.i(TAG, "Создали SoundPool для главной кнопки");
        }

        @SuppressWarnings("deprecation")
        private void createOldSoundPool() {
            mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
            Log.i(TAG, "Создали старый SoundPool для главной кнопки");
        }

        protected void ScorePlus() {
            CurrentScore += 100;
            MyBar.ScoreView.setText(String.format(getResources().getString(R.string.score), CurrentScore));
        }

        protected void MBAnimation(final boolean bool) {
            if (bool) {
                MainButton.setBackgroundResource(R.drawable.mainbutton_green);
            } else {
                MainButton.setBackgroundResource(R.drawable.mainbutton_red);
                MyBar.HeartImage.setBackgroundResource(R.drawable.brokedheart);
            }
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    MainButton.setBackgroundResource(R.drawable.mainbuttonselector);
                    if (!bool) {
                        MyBar.HeartImage.setBackgroundResource(R.drawable.heart);
                    }
                }
            }, 1000);
        }

		//событие - нажали не ту клавишу
        protected void whenRightNoteHaveBeenPlayed() {
            MBAnimation(true);
            ScorePlus();
            octavenumber = randomOctave();
            notenumber = randomNote();
            MainMusicButton(octavenumber, notenumber);
        }

		//событие - нажали нужную клавишу
        protected void whenWrongNoteHaveBeenPlayed() {
            if (Lives > 0) {
                MBAnimation(false);
                Lives -= 1;
                MyBar.HeartScore.setText(String.format(getResources().getString(R.string.lives), Lives));
            } else {
                Intent intent = new Intent(MainActivity.this, HighScoresActivity.class);
                intent.putExtra("CurrentScore", CurrentScore);
                startActivity(intent);
            }
        }

		//обработчик нажатий
        public boolean onTouch(View v, MotionEvent event) {
            int eventAction = event.getAction();
            if (v.getId() == MainButton.getId()) {
                if (eventAction == MotionEvent.ACTION_UP) {
                    // Отпускаем на кнопку
                    v.setPressed(false);
                    Log.i(TAG, "Сыграли секретную ноту");
                }
                if (eventAction == MotionEvent.ACTION_DOWN) {
                    // Нажимаем кнопку
                    mSoundPool.play(mymainnote, 1, 1, 0, 0, 1);
                    v.setPressed(true);
                }
            } else {
                for (int j = 0; j < 12; j++) {
                    if (v.getId() == ArrayOfIDs[j]) {
                        if (eventAction == MotionEvent.ACTION_DOWN) {
                            // Нажимаем кнопку
                            spOfNotes[octavenumber][j].play(keyNote[octavenumber][j], 1, 1, 0, 0, 1);
                            v.setPressed(true);
                            if (j == notenumber) {
                                whenRightNoteHaveBeenPlayed();
                            } else {
                                whenWrongNoteHaveBeenPlayed();
                            }
                        } else {
                            if (eventAction == MotionEvent.ACTION_UP) {
                                //Отпускаем кнопку
                                v.setPressed(false);
                                Log.i(TAG, "Сыграли ноту номер " + Integer.toString(j));
                            }
                            break;
                        }
                    }
                }
            }
            return true;
        }

    }

	//рестарт
    @Override
    protected void onRestart() {
        super.onRestart();
        startnewgame();
        int buyAction = SharedPref.getInstance(this).getIntPreference(SharedPref.APP_BUY_ACTION, 0);
        if (buyAction == 1) {
            buy();
            SharedPref.getInstance(this).writeIntPreference(SharedPref.APP_BUY_ACTION, 0);
        }
        Log.i(TAG, "RESTARTED");
    }

	//новая игра
    protected void startnewgame() {
        setGameMode();
        MyKeyBoard.octavenumber = randomOctave();
        MyKeyBoard.notenumber = randomNote();
        MyKeyBoard.MainMusicButton(MyKeyBoard.octavenumber, MyKeyBoard.notenumber);
        Lives = 5;
        MyBar.HeartScore.setText(String.format(getResources().getString(R.string.lives), Lives));
        CurrentScore = 0;
        MyBar.ScoreView.setText(String.format(getResources().getString(R.string.score), CurrentScore));
        if (!PreferencesHelper.isAdsDisabled()) adShow(!PreferencesHelper.isAdsDisabled());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "Загружаем меню");
        getMenuInflater().inflate(R.menu.mymenu, menu);
        Log.i(TAG, "Загрузили меню, возвращаем значение");
        return true;
    }

	//меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newgame: {
                startnewgame();
                break;
            }
            case R.id.easymode: {
                SharedPref.getInstance(this).writeIntPreference(SharedPref.APP_PREFERENCES_LEVEL, 0);
                startnewgame();
                break;
            }
            case R.id.mediummode: {
                SharedPref.getInstance(this).writeIntPreference(SharedPref.APP_PREFERENCES_LEVEL, 1);
                startnewgame();
                break;
            }
            case R.id.hardmode: {
                SharedPref.getInstance(this).writeIntPreference(SharedPref.APP_PREFERENCES_LEVEL, 2);
                startnewgame();
                break;
            }
            case R.id.expertmode: {
                SharedPref.getInstance(this).writeIntPreference(SharedPref.APP_PREFERENCES_LEVEL, 3);
                startnewgame();
                break;
            }
            case R.id.helpmenu: {
                DialogFragment helpDialog = new HelpDialog();
                helpDialog.show(getFragmentManager(), "help");
                break;
            }
            case R.id.adFreeMenu: {
                if (!PreferencesHelper.isAdsDisabled())
                    buy();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
