package kin.sdk.core.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import kin.sdk.core.Balance;

/**
 * Responsible for presenting details about the account
 * Public address, account balance, account pending balance
 * and in future we will add here button to backup the account (show usage of exportKeyStore)
 * In addition there is "Send Transaction" button here that will navigate to TransactionActivity
 */
public class WalletActivity extends BaseActivity {

    public static final String TAG = WalletActivity.class.getSimpleName();
    public static final String URL_GET_KIN = "http://kin-faucet.rkik.prod/send?public_address=";

    public static Intent getIntent(Context context) {
        return new Intent(context, WalletActivity.class);
    }

    private TextView balance, pendingBalance, publicKey;
    private View balanceProgress, pendingBalanceProgress;
    private DisplayCallback<Balance> balanceCallback, pendingBalanceCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_activity);
        initWidgets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePublicKey();
        updateBalance();
        updatePendingBalance();
    }

    private void initWidgets() {
        balance = (TextView) findViewById(R.id.balance);
        pendingBalance = (TextView) findViewById(R.id.pending_balance);
        publicKey = (TextView) findViewById(R.id.public_key);

        balanceProgress = findViewById(R.id.balance_progress);
        pendingBalanceProgress = findViewById(R.id.pending_balance_progress);

        final View transaction = findViewById(R.id.send_transaction_btn);
        final View refresh = findViewById(R.id.refresh_btn);
        final View getKin = findViewById(R.id.get_kin_btn);
        final View exportKeyStore = findViewById(R.id.export_key_store_btn);

        if (isMainNet()) {
            transaction.setBackgroundResource(R.drawable.button_main_network_bg);
            refresh.setBackgroundResource(R.drawable.button_main_network_bg);
            exportKeyStore.setBackgroundResource(R.drawable.button_main_network_bg);
            getKin.setVisibility(View.GONE);
        } else {
            getKin.setVisibility(View.VISIBLE);
            getKin.setOnClickListener(view -> getKin());
        }

        transaction.setOnClickListener(view -> startActivity(TransactionActivity.getIntent(WalletActivity.this)));
        refresh.setOnClickListener(view -> {
            updateBalance();
            updatePendingBalance();
        });

        exportKeyStore.setOnClickListener(view -> {
            startActivity(ExportKeystoreActivity.getIntent(this));
        });
    }

    private void getKin() {
        final String publicAddress = getKinClient().getAccount().getPublicAddress();
        final String url = URL_GET_KIN + publicAddress;
        final RequestQueue queue = Volley.newRequestQueue(this);
        final StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            response -> updatePendingBalance(),
            error -> ViewUtils.alert(WalletActivity.this, error.getMessage()));
        queue.add(stringRequest);
    }

    private void updatePublicKey() {
        publicKey.setText(getKinClient().getAccount().getPublicAddress());
    }

    private void updateBalance() {
        balanceProgress.setVisibility(View.VISIBLE);
        balanceCallback = new DisplayCallback<Balance>(balanceProgress, balance) {
            @Override
            public void displayResult(Context context, View view, Balance result) {
                ((TextView) view).setText(result.value(0));
            }
        };
        getKinClient().getAccount().getBalance(balanceCallback);
    }

    private void updatePendingBalance() {
        pendingBalanceProgress.setVisibility(View.VISIBLE);
        pendingBalanceCallback = new DisplayCallback<Balance>(pendingBalanceProgress, pendingBalance) {
            @Override
            public void displayResult(Context context, View view, Balance result) {
                ((TextView) view).setText(result.value(0));
            }
        };
        getKinClient().getAccount().getPendingBalance(pendingBalanceCallback);
    }

    @Override
    Intent getBackIntent() {
        return ChooseNetworkActivity.getIntent(this);
    }

    @Override
    int getActionBarTitleRes() {
        return R.string.balance;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingBalanceCallback != null) {
            pendingBalanceCallback.onDetach();
        }
        if (balanceCallback != null) {
            balanceCallback.onDetach();
        }
        pendingBalance = null;
        balance = null;
    }
}
