const crypto = require('crypto');
const seedrandom = require('./seedrandom');

function genSeedText() {
    const { RNG_SEED } = process.env;
    if (RNG_SEED) {
        // Explicit seed is specified so use it
        return RNG_SEED;
    }
    const { GITHUB_PR_NUMBER } = process.env;
    if (GITHUB_PR_NUMBER) {
        // GitHub pull request so use the GITHUB_REF as the seed to ensure consistency throughout PR lifecycle
        return 'pr_' + GITHUB_PR_NUMBER;
    }
    // Otherwise generate a random seed. Note that we do not actually care 
    return 'seed_' + Date.now() + '_' + crypto.randomBytes(16).toString('hex');
}

function createRNG() {
    const seedText = genSeedText();
    const rng = new seedrandom(seedText);

    console.log('::group::RNG Seed');
    console.log('Initialized RNG with RNG_SEED = %s', seedText);    
    console.log('::endgroup::');
    return {
        /**
         * Generate a random number from 0 to 1.
         */
        random: () => rng(),
    };
}

module.exports.RNG = createRNG();
