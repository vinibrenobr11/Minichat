package com.ifsphto.vlp_info2_2017.minichat.page;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.ifsphto.vlp_info2_2017.minichat.LoginActivity;
import com.ifsphto.vlp_info2_2017.minichat.R;
import com.ifsphto.vlp_info2_2017.minichat.connection.ConnectionClass;
import com.ifsphto.vlp_info2_2017.minichat.connection.FTPConnection;
import com.ifsphto.vlp_info2_2017.minichat.services.DownloadService;
import com.ifsphto.vlp_info2_2017.minichat.object.Post;
import com.ifsphto.vlp_info2_2017.minichat.page.adapters.PostsAdapter;
import com.ifsphto.vlp_info2_2017.minichat.settings.SettingsActivity;

import org.apache.commons.net.ftp.FTPClient;

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

    // Botões flutuantes
    private FloatingActionMenu fab_menu;
    private FloatingActionButton new_msg;

    // Listas e Adapters
    private ArrayList<Post> posts;
    private ListView mPosts;

    // Conexão
    private ConnectionClass connectionClass = new ConnectionClass();

    private SwipeRefreshLayout myRefresh;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);

        // Cria e seta um título à barra superior
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.title_activity_user));

        // Cria um Array e recupera a ListView
        posts = new ArrayList<>();
        mPosts = findViewById(R.id.posts);

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
        TextView userId = headerView.findViewById(R.id.UserId);
        TextView userEmail = headerView.findViewById(R.id.UserEmail);

        // Seta o nome de usuario e email nos TextView da barra lateral,
        // obtido atráves do arquivo SharedPreferences
        userId.setText(prefs.getString("name", "Error"));
        userEmail.setText(prefs.getString("email", "Error"));

        myRefresh = findViewById(R.id.swiperefresh);
        myRefresh.setColorSchemeColors(Color.RED, Color.BLUE, Color.GREEN,
                Color.YELLOW, Color.MAGENTA);

        myRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new GetPosts().execute("");
            }
        });

        myRefresh.setRefreshing(true);
        new GetPosts().execute("");
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

            case R.id.nav_messages:
                // TODO: Abrir layout das mensagens, tipo WhatsApp
                break;
            case R.id.logout:
                // Desloga
                logOut();
                break;
            case R.id.check_update_drawer:
                // Verifica Atualização
                verifyUpdate();
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

    /**
     * Verifica se há uma versão mais recente do app disponivel, e se sim, exibe um
     * dialog perguntando se o usuário deseja baixa-lá
     */
    public void verifyUpdate() {

        // Inicia a thread para verificar
        final Thread v = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    // Conecta ao FTP e obtém um nome de um arquivo que contém a versão mais
                    // Recente do app
                    FTPClient ftp = FTPConnection.getConnection();
                    final short newVersion = Short.parseShort(ftp.listNames()[0]);

                    // Obtém a versão do app instalado
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    final short thisVersion = (short) pInfo.versionCode;

                    FTPConnection.closeConnection(ftp);

                    // Se existir uma versão mais nova, um dialogo é feito, se não
                    // um toast avisa que o usuário está na versão mais recente
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Object o = makeSomeWarning(newVersion > thisVersion
                                    , thisVersion, newVersion);

                            if (o instanceof AlertDialog.Builder)
                                ((AlertDialog.Builder) o).create().show();
                            else
                                ((Toast) o).show();

                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * Produz um {@link AlertDialog.Builder} ou {@link Toast} com textos setados
             * dependendo dos parametros passados
             * @param op Se há uma versão mais recente do app disponivel, caso true, é retornado o
             *           Dialog
             * @param t Versão atual do app
             * @param n Versão nova do app
             * @return Object podendo ser {@link AlertDialog.Builder} ou {@link Toast}.
             */
            private Object makeSomeWarning(boolean op, short t, short n) {

                if (op) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainPage.this);

                    builder.setTitle(getString(R.string.update_av_title))

                            .setMessage(getString(R.string.update_av_msg)
                                    + getString(R.string.update_this_ver, t)
                                    + getString(R.string.update_new_ver, n))

                            // Se o usuário clicar em sim, o serviço para baixar é iniciado
                            .setPositiveButton(getString(R.string.ofcourse),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            startService(new Intent(MainPage.this,
                                                    DownloadService.class));

                                            Toast.makeText(getApplicationContext(),
                                                    getString(R.string.downloading_literaly)
                                                    , Toast.LENGTH_LONG).show();
                                        }
                                    })

                            .setNegativeButton(getString(R.string.not_now), null);

                    return builder;
                }

                return Toast.makeText(MainPage.this, getString(R.string.up_to_date)
                        , Toast.LENGTH_LONG);
            }
        });

        v.start();

        // Se a verificação demorar mais de 15 segundos, a thread correspondente é interrompida
        // e uma toast alertando erro é exibido
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(15000);

                    if (v.isAlive()) {
                        v.interrupt();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), getString(R.string.time_exceeded),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        ald.setMessage(getString(R.string.confirm_ald_title));
        ald.setNeutralButton(getString(R.string.no), null);
        // Define o botão positivo do dialogo e qual sua ação
        ald.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

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
            }
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
            Toast.makeText(this, getString(R.string.post_sucess), Toast.LENGTH_LONG).show();
            myRefresh.setRefreshing(true);
            new GetPosts().execute("");
        }
    }

    private class GetPosts extends AsyncTask<String,String,String> {
        String z = "";
        ResultSet rs;
        boolean isSuccess = false;

        AlertDialog.Builder dlg;
        Connection con;

        @Override
        protected void onPostExecute(String s) {

            try {

                dlg.setMessage(z);

                // Obtém os posts e os adicionam na List
                if (isSuccess) {

                    posts.clear();
                    PostsAdapter adp_posts = new PostsAdapter(posts, MainPage.this, R.layout.post_item);
                    rs.first();

                    int i = 0;

                    while (true) {
                        if (i != 0)
                            rs.next();
                        Post post = new Post(rs.getString(1), rs.getString(2), rs.getString(3));
                        posts.add(i, post);
                        i++;
                        if (rs.isLast()) {
                            mPosts.setAdapter(adp_posts);
                            Log.i("Posts Added", i + " were add");
                            toolbar.setSubtitle(i + " " + getString(R.string.posts));
                            break;
                        }
                    }
                } else
                    dlg.show();

            } catch (Exception e) {
                e.printStackTrace();
                dlg.show();
            } finally {
                myRefresh.setRefreshing(false);
                try {
                    if (con != null)
                        con.close();
                } catch (SQLException ignored) {}
            }
        }

        @Override
        protected String doInBackground(String... params) {

            dlg = new AlertDialog.Builder(MainPage.this);
            dlg.setNeutralButton("OK", null);

            try {

                con = connectionClass.conn(false);

                if (con == null)
                    z = connectionClass.getException();
                else {

                    String query = "SELECT * FROM Posts_form";

                    Statement stmt = con.createStatement();
                    rs = stmt.executeQuery(query);
                }

                isSuccess = rs.next();

            } catch (Exception ex) {
                z = ex.getMessage();
                ex.printStackTrace();
            }
            return z;
        }
    }
}