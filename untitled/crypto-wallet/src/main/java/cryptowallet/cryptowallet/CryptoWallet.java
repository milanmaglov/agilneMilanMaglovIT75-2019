package cryptowallet.cryptowallet;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class CryptoWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique=true)
    private String email;

    @Column
    private BigDecimal BTC_amount;

    @Column
    private BigDecimal ETH_amount;

    @Column
    private BigDecimal LTC_amount;

    @Column
    private BigDecimal XRP_amount;


    private String environment;

    public CryptoWallet() {

    }

    public CryptoWallet(String email, BigDecimal BTC_amount, BigDecimal ETH_amount, BigDecimal LTC_amount, BigDecimal XRP_amount, String environment) {
        this.email = email;
        this.BTC_amount = BTC_amount;
        this.ETH_amount = ETH_amount;
        this.LTC_amount = LTC_amount;
        this.XRP_amount = XRP_amount;
        this.environment = environment;
    }

    public CryptoWallet(BigDecimal BTC_amount, BigDecimal ETH_amount, BigDecimal LTC_amount, BigDecimal XRP_amount) {
        this.BTC_amount = BTC_amount;
        this.ETH_amount = ETH_amount;
        this.LTC_amount = LTC_amount;
        this.XRP_amount = XRP_amount;
    }

    public CryptoWallet(String email, String environment) {
        this.email = email;
        BTC_amount = BigDecimal.valueOf(0);
        ETH_amount = BigDecimal.valueOf(0);
        LTC_amount = BigDecimal.valueOf(0);
        XRP_amount = BigDecimal.valueOf(0);
        this.environment = environment;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getBTC_amount() {
        return BTC_amount;
    }

    public void setBTC_amount(BigDecimal BTC_amount) {
        this.BTC_amount = BTC_amount;
    }

    public BigDecimal getETH_amount() {
        return ETH_amount;
    }

    public void setETH_amount(BigDecimal ETH_amount) {
        this.ETH_amount = ETH_amount;
    }

    public BigDecimal getLTC_amount() {
        return LTC_amount;
    }

    public void setLTC_amount(BigDecimal LTC_amount) {
        this.LTC_amount = LTC_amount;
    }

    public BigDecimal getXRP_amount() {
        return XRP_amount;
    }

    public void setXRP_amount(BigDecimal XRP_amount) {
        this.XRP_amount = XRP_amount;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
