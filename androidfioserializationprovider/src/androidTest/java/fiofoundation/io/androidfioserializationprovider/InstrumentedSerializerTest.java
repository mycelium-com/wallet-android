package fiofoundation.io.androidfioserializationprovider;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import android.util.Log;
import fiofoundation.io.fiosdk.errors.serializationprovider.DeserializeAbiError;
import fiofoundation.io.fiosdk.errors.serializationprovider.SerializeAbiError;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class InstrumentedSerializerTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("fiofoundation.io.androidfioserializationprovider.test", appContext.getPackageName());
    }

    @Test
    public void testAbiDeSerializer()
    {
        AbiFIOSerializationProvider provider = new AbiFIOSerializationProvider();

        String hex = "0e656f73696f3a3a6162692f312e30010c6163636f756e745f6e616d65046e616d6505087472616e7366657200040466726f6d0c6163636f756e745f6e616d6502746f0c6163636f756e745f6e616d65087175616e74697479056173736574046d656d6f06737472696e67066372656174650002066973737565720c6163636f756e745f6e616d650e6d6178696d756d5f737570706c79056173736574056973737565000302746f0c6163636f756e745f6e616d65087175616e74697479056173736574046d656d6f06737472696e67076163636f756e7400010762616c616e63650561737365740e63757272656e63795f7374617473000306737570706c790561737365740a6d61785f737570706c79056173736574066973737565720c6163636f756e745f6e616d6503000000572d3ccdcd087472616e73666572bc072d2d2d0a7469746c653a20546f6b656e205472616e736665720a73756d6d6172793a205472616e7366657220746f6b656e732066726f6d206f6e65206163636f756e7420746f20616e6f746865722e0a69636f6e3a2068747470733a2f2f63646e2e746573746e65742e6465762e62316f70732e6e65742f746f6b656e2d7472616e736665722e706e6723636535316566396639656563613334333465383535303765306564343965373666666631323635343232626465643032353566333139366561353963386230630a2d2d2d0a0a2323205472616e73666572205465726d73202620436f6e646974696f6e730a0a492c207b7b66726f6d7d7d2c20636572746966792074686520666f6c6c6f77696e6720746f206265207472756520746f207468652062657374206f66206d79206b6e6f776c656467653a0a0a312e204920636572746966792074686174207b7b7175616e746974797d7d206973206e6f74207468652070726f6365656473206f66206672617564756c656e74206f722076696f6c656e7420616374697669746965732e0a322e2049206365727469667920746861742c20746f207468652062657374206f66206d79206b6e6f776c656467652c207b7b746f7d7d206973206e6f7420737570706f7274696e6720696e6974696174696f6e206f662076696f6c656e636520616761696e7374206f74686572732e0a332e2049206861766520646973636c6f73656420616e7920636f6e747261637475616c207465726d73202620636f6e646974696f6e732077697468207265737065637420746f207b7b7175616e746974797d7d20746f207b7b746f7d7d2e0a0a4920756e6465727374616e6420746861742066756e6473207472616e736665727320617265206e6f742072657665727369626c6520616674657220746865207b7b247472616e73616374696f6e2e64656c61795f7365637d7d207365636f6e6473206f72206f746865722064656c617920617320636f6e66696775726564206279207b7b66726f6d7d7d2773207065726d697373696f6e732e0a0a4966207468697320616374696f6e206661696c7320746f20626520697272657665727369626c7920636f6e6669726d656420616674657220726563656976696e6720676f6f6473206f722073657276696365732066726f6d20277b7b746f7d7d272c204920616772656520746f206569746865722072657475726e2074686520676f6f6473206f72207365727669636573206f7220726573656e64207b7b7175616e746974797d7d20696e20612074696d656c79206d616e6e65722e0000000000a531760569737375650000000000a86cd445066372656174650002000000384f4d113203693634010863757272656e6379010675696e743634076163636f756e740000000000904dc603693634010863757272656e6379010675696e7436340e63757272656e63795f737461747300000000";
        String jsonResult = "{\"version\":\"eosio::abi/1.0\",\"types\":[{\"new_type_name\":\"account_name\",\"type\":\"name\"}],\"structs\":[{\"name\":\"transfer\",\"base\":\"\",\"fields\":[{\"name\":\"from\",\"type\":\"account_name\"},{\"name\":\"to\",\"type\":\"account_name\"},{\"name\":\"quantity\",\"type\":\"asset\"},{\"name\":\"memo\",\"type\":\"string\"}]},{\"name\":\"create\",\"base\":\"\",\"fields\":[{\"name\":\"issuer\",\"type\":\"account_name\"},{\"name\":\"maximum_supply\",\"type\":\"asset\"}]},{\"name\":\"issue\",\"base\":\"\",\"fields\":[{\"name\":\"to\",\"type\":\"account_name\"},{\"name\":\"quantity\",\"type\":\"asset\"},{\"name\":\"memo\",\"type\":\"string\"}]},{\"name\":\"account\",\"base\":\"\",\"fields\":[{\"name\":\"balance\",\"type\":\"asset\"}]},{\"name\":\"currency_stats\",\"base\":\"\",\"fields\":[{\"name\":\"supply\",\"type\":\"asset\"},{\"name\":\"max_supply\",\"type\":\"asset\"},{\"name\":\"issuer\",\"type\":\"account_name\"}]}],\"actions\":[{\"name\":\"transfer\",\"type\":\"transfer\",\"ricardian_contract\":\"---\\ntitle: Token Transfer\\nsummary: Transfer tokens from one account to another.\\nicon: https://cdn.testnet.dev.b1ops.net/token-transfer.png#ce51ef9f9eeca3434e85507e0ed49e76fff1265422bded0255f3196ea59c8b0c\\n---\\n\\n## Transfer Terms & Conditions\\n\\nI, {{from}}, certify the following to be true to the best of my knowledge:\\n\\n1. I certify that {{quantity}} is not the proceeds of fraudulent or violent activities.\\n2. I certify that, to the best of my knowledge, {{to}} is not supporting initiation of violence against others.\\n3. I have disclosed any contractual terms & conditions with respect to {{quantity}} to {{to}}.\\n\\nI understand that funds transfers are not reversible after the {{$transaction.delay_sec}} seconds or other delay as configured by {{from}}'s permissions.\\n\\nIf this action fails to be irreversibly confirmed after receiving goods or services from '{{to}}', I agree to either return the goods or services or resend {{quantity}} in a timely manner.\"},{\"name\":\"issue\",\"type\":\"issue\",\"ricardian_contract\":\"\"},{\"name\":\"create\",\"type\":\"create\",\"ricardian_contract\":\"\"}],\"tables\":[{\"name\":\"accounts\",\"index_type\":\"i64\",\"key_names\":[\"currency\"],\"key_types\":[\"uint64\"],\"type\":\"account\"},{\"name\":\"stat\",\"index_type\":\"i64\",\"key_names\":[\"currency\"],\"key_types\":[\"uint64\"],\"type\":\"currency_stats\"}],\"ricardian_clauses\":[],\"error_messages\":[],\"abi_extensions\":[],\"variants\":[]}";

        String json = null;

        try {
            json = provider.deserializeAbi(hex);

        } catch (DeserializeAbiError err) {
            err.printStackTrace();
        }

        assertNotNull(json);
        assertEquals(json, jsonResult);

    }

    @Test
    public void testAbiSerializer()
    {
        AbiFIOSerializationProvider provider = new AbiFIOSerializationProvider();

        String hexResult = "0e656f73696f3a3a6162692f312e30010c6163636f756e745f6e616d65046e616d6505087472616e7366657200040466726f6d0c6163636f756e745f6e616d6502746f0c6163636f756e745f6e616d65087175616e74697479056173736574046d656d6f06737472696e67066372656174650002066973737565720c6163636f756e745f6e616d650e6d6178696d756d5f737570706c79056173736574056973737565000302746f0c6163636f756e745f6e616d65087175616e74697479056173736574046d656d6f06737472696e67076163636f756e7400010762616c616e63650561737365740e63757272656e63795f7374617473000306737570706c790561737365740a6d61785f737570706c79056173736574066973737565720c6163636f756e745f6e616d6503000000572d3ccdcd087472616e73666572bc072d2d2d0a7469746c653a20546f6b656e205472616e736665720a73756d6d6172793a205472616e7366657220746f6b656e732066726f6d206f6e65206163636f756e7420746f20616e6f746865722e0a69636f6e3a2068747470733a2f2f63646e2e746573746e65742e6465762e62316f70732e6e65742f746f6b656e2d7472616e736665722e706e6723636535316566396639656563613334333465383535303765306564343965373666666631323635343232626465643032353566333139366561353963386230630a2d2d2d0a0a2323205472616e73666572205465726d73202620436f6e646974696f6e730a0a492c207b7b66726f6d7d7d2c20636572746966792074686520666f6c6c6f77696e6720746f206265207472756520746f207468652062657374206f66206d79206b6e6f776c656467653a0a0a312e204920636572746966792074686174207b7b7175616e746974797d7d206973206e6f74207468652070726f6365656473206f66206672617564756c656e74206f722076696f6c656e7420616374697669746965732e0a322e2049206365727469667920746861742c20746f207468652062657374206f66206d79206b6e6f776c656467652c207b7b746f7d7d206973206e6f7420737570706f7274696e6720696e6974696174696f6e206f662076696f6c656e636520616761696e7374206f74686572732e0a332e2049206861766520646973636c6f73656420616e7920636f6e747261637475616c207465726d73202620636f6e646974696f6e732077697468207265737065637420746f207b7b7175616e746974797d7d20746f207b7b746f7d7d2e0a0a4920756e6465727374616e6420746861742066756e6473207472616e736665727320617265206e6f742072657665727369626c6520616674657220746865207b7b247472616e73616374696f6e2e64656c61795f7365637d7d207365636f6e6473206f72206f746865722064656c617920617320636f6e66696775726564206279207b7b66726f6d7d7d2773207065726d697373696f6e732e0a0a4966207468697320616374696f6e206661696c7320746f20626520697272657665727369626c7920636f6e6669726d656420616674657220726563656976696e6720676f6f6473206f722073657276696365732066726f6d20277b7b746f7d7d272c204920616772656520746f206569746865722072657475726e2074686520676f6f6473206f72207365727669636573206f7220726573656e64207b7b7175616e746974797d7d20696e20612074696d656c79206d616e6e65722e0000000000a531760569737375650000000000a86cd445066372656174650002000000384f4d113203693634010863757272656e6379010675696e743634076163636f756e740000000000904dc603693634010863757272656e6379010675696e7436340e63757272656e63795f737461747300000000";
        String json = "{\"version\":\"eosio::abi/1.0\",\"types\":[{\"new_type_name\":\"account_name\",\"type\":\"name\"}],\"structs\":[{\"name\":\"transfer\",\"base\":\"\",\"fields\":[{\"name\":\"from\",\"type\":\"account_name\"},{\"name\":\"to\",\"type\":\"account_name\"},{\"name\":\"quantity\",\"type\":\"asset\"},{\"name\":\"memo\",\"type\":\"string\"}]},{\"name\":\"create\",\"base\":\"\",\"fields\":[{\"name\":\"issuer\",\"type\":\"account_name\"},{\"name\":\"maximum_supply\",\"type\":\"asset\"}]},{\"name\":\"issue\",\"base\":\"\",\"fields\":[{\"name\":\"to\",\"type\":\"account_name\"},{\"name\":\"quantity\",\"type\":\"asset\"},{\"name\":\"memo\",\"type\":\"string\"}]},{\"name\":\"account\",\"base\":\"\",\"fields\":[{\"name\":\"balance\",\"type\":\"asset\"}]},{\"name\":\"currency_stats\",\"base\":\"\",\"fields\":[{\"name\":\"supply\",\"type\":\"asset\"},{\"name\":\"max_supply\",\"type\":\"asset\"},{\"name\":\"issuer\",\"type\":\"account_name\"}]}],\"actions\":[{\"name\":\"transfer\",\"type\":\"transfer\",\"ricardian_contract\":\"---\\ntitle: Token Transfer\\nsummary: Transfer tokens from one account to another.\\nicon: https://cdn.testnet.dev.b1ops.net/token-transfer.png#ce51ef9f9eeca3434e85507e0ed49e76fff1265422bded0255f3196ea59c8b0c\\n---\\n\\n## Transfer Terms & Conditions\\n\\nI, {{from}}, certify the following to be true to the best of my knowledge:\\n\\n1. I certify that {{quantity}} is not the proceeds of fraudulent or violent activities.\\n2. I certify that, to the best of my knowledge, {{to}} is not supporting initiation of violence against others.\\n3. I have disclosed any contractual terms & conditions with respect to {{quantity}} to {{to}}.\\n\\nI understand that funds transfers are not reversible after the {{$transaction.delay_sec}} seconds or other delay as configured by {{from}}'s permissions.\\n\\nIf this action fails to be irreversibly confirmed after receiving goods or services from '{{to}}', I agree to either return the goods or services or resend {{quantity}} in a timely manner.\"},{\"name\":\"issue\",\"type\":\"issue\",\"ricardian_contract\":\"\"},{\"name\":\"create\",\"type\":\"create\",\"ricardian_contract\":\"\"}],\"tables\":[{\"name\":\"accounts\",\"index_type\":\"i64\",\"key_names\":[\"currency\"],\"key_types\":[\"uint64\"],\"type\":\"account\"},{\"name\":\"stat\",\"index_type\":\"i64\",\"key_names\":[\"currency\"],\"key_types\":[\"uint64\"],\"type\":\"currency_stats\"}],\"ricardian_clauses\":[],\"error_messages\":[],\"abi_extensions\":[],\"variants\":[]}";

        String hex = null;

        try {
            hex = provider.serializeAbi(json);

            Log.i("AbiFIOSerializationProvider",hex);

            testAbiDeSerializer(hex);

        } catch (SerializeAbiError err) {
            err.printStackTrace();
        }

        assertNotNull(hex);
        assertEquals(hex.toLowerCase(), hexResult);

    }

    private void testAbiDeSerializer(String hex)
    {
        AbiFIOSerializationProvider provider = new AbiFIOSerializationProvider();

        //String hex = "0e656f73696f3a3a6162692f312e30010c6163636f756e745f6e616d65046e616d6505087472616e7366657200040466726f6d0c6163636f756e745f6e616d6502746f0c6163636f756e745f6e616d65087175616e74697479056173736574046d656d6f06737472696e67066372656174650002066973737565720c6163636f756e745f6e616d650e6d6178696d756d5f737570706c79056173736574056973737565000302746f0c6163636f756e745f6e616d65087175616e74697479056173736574046d656d6f06737472696e67076163636f756e7400010762616c616e63650561737365740e63757272656e63795f7374617473000306737570706c790561737365740a6d61785f737570706c79056173736574066973737565720c6163636f756e745f6e616d6503000000572d3ccdcd087472616e73666572bc072d2d2d0a7469746c653a20546f6b656e205472616e736665720a73756d6d6172793a205472616e7366657220746f6b656e732066726f6d206f6e65206163636f756e7420746f20616e6f746865722e0a69636f6e3a2068747470733a2f2f63646e2e746573746e65742e6465762e62316f70732e6e65742f746f6b656e2d7472616e736665722e706e6723636535316566396639656563613334333465383535303765306564343965373666666631323635343232626465643032353566333139366561353963386230630a2d2d2d0a0a2323205472616e73666572205465726d73202620436f6e646974696f6e730a0a492c207b7b66726f6d7d7d2c20636572746966792074686520666f6c6c6f77696e6720746f206265207472756520746f207468652062657374206f66206d79206b6e6f776c656467653a0a0a312e204920636572746966792074686174207b7b7175616e746974797d7d206973206e6f74207468652070726f6365656473206f66206672617564756c656e74206f722076696f6c656e7420616374697669746965732e0a322e2049206365727469667920746861742c20746f207468652062657374206f66206d79206b6e6f776c656467652c207b7b746f7d7d206973206e6f7420737570706f7274696e6720696e6974696174696f6e206f662076696f6c656e636520616761696e7374206f74686572732e0a332e2049206861766520646973636c6f73656420616e7920636f6e747261637475616c207465726d73202620636f6e646974696f6e732077697468207265737065637420746f207b7b7175616e746974797d7d20746f207b7b746f7d7d2e0a0a4920756e6465727374616e6420746861742066756e6473207472616e736665727320617265206e6f742072657665727369626c6520616674657220746865207b7b247472616e73616374696f6e2e64656c61795f7365637d7d207365636f6e6473206f72206f746865722064656c617920617320636f6e66696775726564206279207b7b66726f6d7d7d2773207065726d697373696f6e732e0a0a4966207468697320616374696f6e206661696c7320746f20626520697272657665727369626c7920636f6e6669726d656420616674657220726563656976696e6720676f6f6473206f722073657276696365732066726f6d20277b7b746f7d7d272c204920616772656520746f206569746865722072657475726e2074686520676f6f6473206f72207365727669636573206f7220726573656e64207b7b7175616e746974797d7d20696e20612074696d656c79206d616e6e65722e0000000000a531760569737375650000000000a86cd445066372656174650002000000384f4d113203693634010863757272656e6379010675696e743634076163636f756e740000000000904dc603693634010863757272656e6379010675696e7436340e63757272656e63795f737461747300000000";
        String jsonResult = "{\"version\":\"eosio::abi/1.0\",\"types\":[{\"new_type_name\":\"account_name\",\"type\":\"name\"}],\"structs\":[{\"name\":\"transfer\",\"base\":\"\",\"fields\":[{\"name\":\"from\",\"type\":\"account_name\"},{\"name\":\"to\",\"type\":\"account_name\"},{\"name\":\"quantity\",\"type\":\"asset\"},{\"name\":\"memo\",\"type\":\"string\"}]},{\"name\":\"create\",\"base\":\"\",\"fields\":[{\"name\":\"issuer\",\"type\":\"account_name\"},{\"name\":\"maximum_supply\",\"type\":\"asset\"}]},{\"name\":\"issue\",\"base\":\"\",\"fields\":[{\"name\":\"to\",\"type\":\"account_name\"},{\"name\":\"quantity\",\"type\":\"asset\"},{\"name\":\"memo\",\"type\":\"string\"}]},{\"name\":\"account\",\"base\":\"\",\"fields\":[{\"name\":\"balance\",\"type\":\"asset\"}]},{\"name\":\"currency_stats\",\"base\":\"\",\"fields\":[{\"name\":\"supply\",\"type\":\"asset\"},{\"name\":\"max_supply\",\"type\":\"asset\"},{\"name\":\"issuer\",\"type\":\"account_name\"}]}],\"actions\":[{\"name\":\"transfer\",\"type\":\"transfer\",\"ricardian_contract\":\"---\\ntitle: Token Transfer\\nsummary: Transfer tokens from one account to another.\\nicon: https://cdn.testnet.dev.b1ops.net/token-transfer.png#ce51ef9f9eeca3434e85507e0ed49e76fff1265422bded0255f3196ea59c8b0c\\n---\\n\\n## Transfer Terms & Conditions\\n\\nI, {{from}}, certify the following to be true to the best of my knowledge:\\n\\n1. I certify that {{quantity}} is not the proceeds of fraudulent or violent activities.\\n2. I certify that, to the best of my knowledge, {{to}} is not supporting initiation of violence against others.\\n3. I have disclosed any contractual terms & conditions with respect to {{quantity}} to {{to}}.\\n\\nI understand that funds transfers are not reversible after the {{$transaction.delay_sec}} seconds or other delay as configured by {{from}}'s permissions.\\n\\nIf this action fails to be irreversibly confirmed after receiving goods or services from '{{to}}', I agree to either return the goods or services or resend {{quantity}} in a timely manner.\"},{\"name\":\"issue\",\"type\":\"issue\",\"ricardian_contract\":\"\"},{\"name\":\"create\",\"type\":\"create\",\"ricardian_contract\":\"\"}],\"tables\":[{\"name\":\"accounts\",\"index_type\":\"i64\",\"key_names\":[\"currency\"],\"key_types\":[\"uint64\"],\"type\":\"account\"},{\"name\":\"stat\",\"index_type\":\"i64\",\"key_names\":[\"currency\"],\"key_types\":[\"uint64\"],\"type\":\"currency_stats\"}],\"ricardian_clauses\":[],\"error_messages\":[],\"abi_extensions\":[],\"variants\":[]}";

        String json = null;

        try {
            json = provider.deserializeAbi(hex);

            Log.i("AbiFIOSerializationProvider",json);
        } catch (DeserializeAbiError err) {
            err.printStackTrace();
        }

        assertNotNull(json);
        assertEquals(json, jsonResult);

    }
}
