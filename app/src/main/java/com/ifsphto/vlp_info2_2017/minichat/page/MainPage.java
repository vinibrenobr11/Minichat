package com.ifsphto.vlp_info2_2017.minichat.page;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.ifsphto.vlp_info2_2017.minichat.LoginActivity;
import com.ifsphto.vlp_info2_2017.minichat.R;
import com.ifsphto.vlp_info2_2017.minichat.connection.NSDConnection;
import com.ifsphto.vlp_info2_2017.minichat.services.MessageService;
import com.ifsphto.vlp_info2_2017.minichat.settings.SettingsActivity;

/**
 * Essa classe é, por enquanto a maior do projeto, ela é a pagina inicial
 * onde o usuário vê os posts de outros usuários
 *
 * Nessa classe principalmente iremos implementar o P2P
 */

// TODO: 22/10/2017  Retirar ProgressDialog de todas as classes

public class MainPage extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    /*
     Nome do campo dentro do arquivo shared preferences
     ele diz se o usuário está logado atraves de um boolean
     essa string diz o nome desse campo
     */
    public static final String PREF_LOG = "isLoggedIn";

    // Objeto representando o arquivo SharedPreferences
    private SharedPreferences prefs;
    private String name;

    // Botões flutuantes
    private FloatingActionMenu fab_menu;
    private FloatingActionButton new_msg;

    // Listas e Adapters
    private ListView mDevs;

    private NSDConnection nsdConn;
    private ArrayAdapter<NsdServiceInfo> devs;

    private TextView userId;
    private TextView userEmail;

    @Override
    protected void onDestroy() {
        nsdConn.finishEverything();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);

        // Cria e seta um título à barra superior
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getString(R.string.title_activity_user));

        // Cria um Array e recupera a ListView
        mDevs = findViewById(R.id.posts);

        // Recupera o arquivo SharedPreferences
        prefs = getSharedPreferences(LoginActivity.LOGIN_PREFS, MODE_PRIVATE);

        // Obtém os 3 botões flutuantes, 2 que são ativados ao clicar no maior
        fab_menu = findViewById(R.id.fab_menu);
        FloatingActionButton new_post = findViewById(R.id.new_post);
        new_msg = findViewById(R.id.new_msg);

        /*
        Aqui é setado que ao clicar fora dos botões enquanto eles estiverem visíveis,
        eles serão fechados.

        Essa classe implementa a interface View.OnClickListener
        por isso o parâmetro 'this' é passado
         */
        fab_menu.setClosedOnTouchOutside(true);
        new_post.setOnClickListener(this);
        new_msg.setOnClickListener(this);

        // Não sei
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Obtém o menu lateral e seta um Listener.
        // Essa classe implementa NavigationView.OnNavigationItemSelectedListener
        // por isso o 'this'
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);

        // Recupera os TextView que estão na imagem da barra lateral
        userId = headerView.findViewById(R.id.UserId);
        userEmail = headerView.findViewById(R.id.UserEmail);

        // Seta o nome de usuario e email nos TextView da barra lateral,
        // obtido atráves do arquivo SharedPreferences
        name = prefs.getString("name", "Undefined");

        SwipeRefreshLayout myRefresh = findViewById(R.id.swiperefresh);
        myRefresh.setColorSchemeColors(Color.RED, Color.BLUE, Color.GREEN,
                Color.YELLOW, Color.MAGENTA);

        myRefresh.setOnRefreshListener(() -> {
        });

        nsdConn = new NSDConnection(this);
        nsdConn.register(name);

        mDevs.setOnItemClickListener((adapterView, view, i, l) -> {

            try {
                NsdServiceInfo resolved = nsdConn.resolve(devs.getItem(i));
                Intent it = new Intent(this, ChatActivity.class);
                it.putExtra("ServiceInfo", resolved);

                startActivity(it);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Deu ruim", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void nameHasCollided() {

        EditText edt_name = new EditText(this);
        edt_name.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.MATCH_PARENT));

        AlertDialog.Builder new_name = new AlertDialog.Builder(this);
        new_name.setTitle("Opa, deu merda").setMessage("Há alguem com o mesmo nome na rede. É " +
                "melhor muda-lo: ").setPositiveButton("OK", (dialogInterface, i) -> {

            String naime = edt_name.getText().toString();

            nsdConn.register(naime);

            SharedPreferences.Editor ed = prefs.edit();
            ed.putString("name", naime);
            ed.apply();

        }).setView(edt_name).create().show();
    }

    /**
    O método onBackPressed é padrão do Android e sempre é executado
    quando o botão de voltar é pressionado

    Aqui ele é modificado
     */
    @Override
    public void onBackPressed() {

        // Obtém o layout da barra lateral
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START) || fab_menu.isOpened()) {
            // Se o menu estiver aberto ou os botões estiverem á vista, eles serão fechado
            drawer.closeDrawer(GravityCompat.START);
            fab_menu.close(true);
        }
        else
            // Se não, o método padrão é executado
            super.onBackPressed();
    }

    // Este método é executado quando alguma opção no painel lateral é escolhida
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        // Verifica qual foi a escolha
        switch (item.getItemId()) {

            case R.id.home:
                break;
            case R.id.nav_messages:
                // TODO: Abrir layout das mensagens, tipo WhatsApp
                break;
            case R.id.logout:
                // Desloga
                logOut();
                break;
            case R.id.drawer_preferences:
                // Inicia a tela de configurações
                startActivityForResult(new Intent(this, SettingsActivity.class), 100);
                break;
        }

        // Recupera o painel e o fecha
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_refresh:
                discover();
        }

        return super.onOptionsItemSelected(item);
    }

    private void discover() {
        new Thread(() -> {

            try {
                nsdConn.discover();
            } catch (Exception e) {
                MainPage.this.runOnUiThread(() ->
                        Toast.makeText(getApplicationContext(), R.string.refresh_alr_running, Toast.LENGTH_LONG)
                                .show());
            }

            while (true) {

                try {
                    synchronized (nsdConn.synchronize) {
                        nsdConn.synchronize.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                devs = nsdConn.getDevices();

                if (devs.getCount() != mDevs.getCount()) {
                    MainPage.this.runOnUiThread(() -> {
                        mDevs.setAdapter(devs);
                        Toast.makeText(getApplicationContext(), "Adapter setado"
                                , Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    /**
     * Desloga o usuário
     */
    public void logOut() {

        // Cria um dialogo perguntando se o usuário tem certeza
        AlertDialog.Builder ald = new AlertDialog.Builder(this);
        ald.setMessage(R.string.confirm_ald_title);
        ald.setNeutralButton(R.string.no, null);
        // Define o botão positivo do dialogo e qual sua ação
        ald.setPositiveButton(R.string.yes, (dialog, which) -> {

            // Cria um Intent para a Activity LoginActivity
            final Intent it = new Intent(MainPage.this, LoginActivity.class);

            // Cria um editor para o arquivo SharedPreferences
            final SharedPreferences.Editor ed = prefs.edit();

            // Define que o usuário não está mais logado
            ed.putBoolean(PREF_LOG, false);

            // Aplica as ediçoes no arquivo e volta para a tela de login
            ed.apply();
            startActivity(it);
            finish();
        });
        ald.show();
    }

    /**
     * Implemetado da interface {@link android.view.View.OnClickListener}
     * @param v View que desencadeou o método
     */
    @Override
    public void onClick(View v) {

        Intent it = new Intent(this, SharingActivity.class);

        // Verifica quem chamou o método, e define dados no Intent
        if (v == new_msg)
            it.putExtra("tab", 1);
        else
            it.putExtra("tab", 0);

        /*
        Inicia a SharingActivity com os dados da aba escolhida
        esperando por um resultado ao retornar
         */
        startActivityForResult(it, 50);
    }

    /*
    Este método é padrão do Android e é executado quando uma Activity volta para
    uma tela anterior que a solicitou
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Chama o que o método faz por padrão
        super.onActivityResult(requestCode, resultCode, data);

        // Verifica se o resultado corresponde ao sucesso
        if (resultCode == 52) {
            Toast.makeText(this, R.string.post_sucess, Toast.LENGTH_LONG).show();
        }
    }

    public void setDrawerText(String name, int port) {
        runOnUiThread(() -> {
            userId.setText(name);
            userEmail.setText(String.valueOf(port));
        });
    }
}