#  BeanChain Genesis Allocation & Emission Mechanics

BeanChain launched with a fixed total supply of **100,000,000 BEAN**, distributed across purpose-bound wallets at genesis.
Each allocation is designed to incentivize participation, ensure network security, and support long-term sustainability 
— all without centralized control over inflation or minting.

---

> ⚠️ **Attention!**
>
> During early testnet deployments, BeanChain was running with an incorrect total supply due to a misconfigured genesis allocation. The total BEAN supply was accidentally capped at **70,000,000 BEAN**, when the intended value was **100,000,000 BEAN**.
>
> As of **[07/09/2025]**, this has been corrected. The total BEAN supply is now permanently and immutably set to:
>
> ### 🎯 **100,000,000 BEAN (HARDCAP)**
>
> This is the **ONLY** time the genesis supply has been modified, and it will **NEVER CHANGE AGAIN!**
>
> All current allocations have been rebalanced to match the intended design. We appreciate the early testers who helped uncover this issue and move us toward a stable mainnet.

---

##  Genesis Wallet Allocations

| Wallet Label      | Address                    | Allocation | Purpose |
|-------------------|----------------------------|------------|---------|
| `FAUCETWALLET`     | `BEANX:0xFAUCETWALLET`      | 10,000,000 | Public faucet drip (handled by RN)  
| `EARLYWALLET`      | `BEANX:0xEARLYWALLET`       | 10,000,000 | Auto-airdrops to new wallets  
| `STAKEREWARD`      | `BEANX:0xSTAKEREWARD`       | 14,000,000 | Validator staking incentives  
| `NODEREWARD`       | `BEANX:0xNODEREWARD`        | 40,000,000 | Uptime rewards for active nodes  
| `TEAM`             | `BEANX:0x1c8496...`         | 6,000,000  | Core dev & ecosystem fund (unlocked at genesis)  
| `LIQUIDITY`        | `BEANX:0xLIQUIDITY`         | 20,000,000 | Reserved for future LPs or DAO allocation  

---

## 🔁 Emission Mechanics by Wallet

### 🟢 `FAUCETWALLET` – Public Drip System
- Controlled by the Reward Node (RN)
- Starts at **100 BEAN drip**
- Decreases with every drip down to **1 BEAN**
- Once depleted, the faucet stops
- Intended for onboarding new users and testers

---

### 🟢 `EARLYWALLET` – Airdrop for New Wallets
- Every new wallet to join the network receives **100 BEAN**
- Automatically distributed on first interaction
- Stops once the wallet is empty

---

### 🟢 `NODEREWARD` – Ping-Based Node Uptime Rewards
- Ping system runs every **10 blocks**
- Active nodes that respond to pings are rewarded
- Starts at **0.01 BEAN per node per reward**
- Halves every **500,000 blocks**
- Continues to drip out until the allocation is exhausted

---

### 🟢 `STAKEREWARD` – Validator Incentive Drip
- Similar to faucet logic, but attached to **each block**
- Each block validator receives a **reward like 200 BEAN**
- This value **continuously decreases** over time
- Eventually drips down to **0.000001 BEAN** and then ends
- Reward is separate from gas fees 

---

### 🟢 `TEAM` – Outlandish Core Allocation
- 6,000,000 BEAN granted to the Outlandish team wallet
- Released at genesis
- Intended for early development, ops, and maintenance
- Transparent and capped — no future inflation

---

### 🔒 `LIQUIDITY` – Reserved for LPs or DAO
- 20,000,000 BEAN held untouched at genesis
- Intended for:
  - Future **liquidity pool seeding** (e.g. BEAN–ETH bridge)
  - Or redirected to **community initiatives** by DAO vote
- Can be burned, redistributed, or used depending on governance

---

## 🧠 Philosophy

BeanChain is built to be:

- **Finite** — no minting beyond 100M BEAN
- **Transparent** — all system wallets are known and labeled
- **Sustainable** — emissions drip down naturally, reducing inflation pressure
- **Decentralized** — community will eventually decide on reserved funds like `LIQUIDITY`

---

## 📌 Notes

- All emission flows (faucet, node rewards, validator incentives) are **on-chain system transactions**, signed by the RN and handled via secure protocols.
- The faucet and validator drip models are **asymptotic**, ensuring early participants receive more while preserving longevity.

---
