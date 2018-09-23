package com.github.nidorx.jtrade.broker.impl.metatrader;

import com.github.nidorx.jtrade.broker.impl.metatrader.model.Topic;
import com.github.nidorx.jtrade.core.Tick;
import com.github.nidorx.jtrade.core.Account;
import com.github.nidorx.jtrade.broker.Broker;
import com.github.nidorx.jtrade.core.Instrument;
import com.github.nidorx.jtrade.core.Rate;
import com.github.nidorx.jtrade.broker.exception.TradeException;
import com.github.nidorx.jtrade.broker.impl.metatrader.model.Command;
import com.github.nidorx.jtrade.broker.trading.Order;
import com.github.nidorx.jtrade.broker.trading.Position;
import com.github.nidorx.jtrade.util.SDParser;
import java.io.IOException;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integração com o Metatrader usando socket
 *
 * https://www.mql5.com/en/articles/1284
 *
 * @author Alex Rodin <contato@alexrodin.info>
 */
public class MetatraderBroker extends Broker {

    private static final Logger LOGGER = Logger.getLogger(MetatraderBroker.class.getName());

    private final Map<String, MT5SocketClient> CLIENTS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        MetatraderBroker metatraderBroker = new MetatraderBroker();
        metatraderBroker.connect("127.0.0.1", 23456);
    }

    /**
     * Permite criar uma conexão com o EA
     *
     * Somente é permitido criar conexões com o EA que estejam operando a mesma conta
     *
     * @param host
     * @param port
     * @throws java.io.IOException
     */
    public void connect(String host, int port) throws IOException {
        if (CLIENTS.containsKey(host + ":" + port)) {
            return;
        }

        final MT5SocketClient client = new MT5SocketClient(host, port);
        client.onConnect(() -> {

            // Obtém informação sobre o instrumento
            while (true) {
                try {
                    // "SYMBOL BASE QUOTE DIGITS CONTRACT_SIZE TICK_VALUE TIME BID ASK"
                    String response = client.exec(Command.SYMBOL);
                    SDParser p = new SDParser(response, ' ');

                    String symbol = p.pop();
                    String base = p.pop();
                    String quote = p.pop();
                    int digits = p.popInt();
                    double contractSize = p.popDouble();
                    double tickValue = p.popDouble();
                    long time = p.popLong();
                    double bid = p.popDouble();
                    double ask = p.popDouble();

                    createInstrument(symbol, base, quote, digits, contractSize, tickValue, bid, ask);

                    setServerTime(Instant.ofEpochSecond(time));
                    break;
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }

            // Observa informações sobre a conta
            client.subscribe(Topic.ACCOUNT, (account) -> {
                this.setAccount((Account) account);
            });

            // Observa novos ticks
            client.subscribe(Topic.TICK, (tick) -> {
                this.processTick((Tick) tick);
            });

            // Observa novos candles
            client.subscribe(Topic.RATES, (rate) -> {
                try {
                    this.processRate((Rate) rate);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            });

            // Sempre que um novo Server for adicionado, faz a conexão com novo server
            client.subscribe(Topic.SERVERS, (servers) -> {
                for (Integer serverPort : (List<Integer>) servers) {
                    if (serverPort.equals(port)) {
                        continue;
                    }
                    try {
                        connect(host, serverPort);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
            });
        });

        client.onDisconnect(() -> {
            System.out.println("Desconectado");
        });

        // Finalmente, conecta-se com o server
        client.connect();

        // Salva referencia para o cliente
        CLIENTS.put(host + ":" + port, client);
    }

    @Override
    public String getName() {
        return "Metatrader";
    }

    @Override
    public Position getPosition(Instrument instrument) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double stopLevel(Instrument instrument) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double freezeLevel(Instrument instrument) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Order buy(Instrument instrument, double price, double volume, long deviation, double sl, double tp) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Order sell(Instrument instrument, double price, double volume, long deviation, double sl, double tp) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Order buyLimit(Instrument instrument, double price, double volume, double sl, double tp) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Order sellLimit(Instrument instrument, double price, double volume, double sl, double tp) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Order sellStop(Instrument instrument, double price, double volume, double sl, double tp) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void modify(Order order, double price, double volume, double sl, double tp) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void remove(Order order) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void modify(Position position, double sl, double tp) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close(Position position, double price, long deviation) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void closePartial(Position position, double price, double volume, long deviation) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Order buyStop(Instrument instrument, double price, double volume, double sl, double tp) throws TradeException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
