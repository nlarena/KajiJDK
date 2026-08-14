// Differential workload for the F3 JIT, step 4 — dimension: **the `wide` prefix**.
//
// Two things put `wide` in front of an instruction, and both used to disqualify a whole method:
//
//   * a **16-bit constant** on `iinc`. `x += 300` is enough — the narrow form's operand is a signed
//     byte. That is why `JtOps.compare` accumulates its result as `r = r + r; r += 1` instead of
//     `r += 1, 2, 4, ...`: a `+= 256` would have made the method uncompilable;
//   * a **16-bit local index**, which needs a frame with more than 256 slots. `deep` below has 262
//     `int` locals, so everything it does to the last few is `wide iload` / `wide istore` — and it
//     is machine-generated for that reason, not written out by hand for fun.
//
// Both are still purely frame-local: the same three instructions with a wider operand field, so
// nothing about the compiled subset's purity argument changes. What the test is really asking is
// whether the decoder gets the operand *widths* right — a 6-byte `wide iinc` read as 4 bytes would
// resynchronise the instruction stream two bytes into the next opcode.
public class WdWide {
    // `wide iinc`: constants that do not fit in a signed byte, in both directions, plus one that
    // does — so a decoder that always took the wide path would fail here too.
    static int bump(int x) {
        for (int i = 0; i < 30; i++) {
            x += 300;
            x -= 456;
            x += 7;
            x += 32767;
            x -= 32768;
        }
        return x;
    }


    // 262 `int` locals. Slots 0..255 are reachable with the narrow opcodes; everything from 256 up
    // needs `wide`, and the tail of the method deliberately does its arithmetic there.
    static int deep(int seed) {
        int v0 = seed;
        int v1 = v0 + -2;
        int v2 = v1 + -1;
        int v3 = v2 + 0;
        int v4 = v3 + 1;
        int v5 = v4 + 2;
        int v6 = v5 + 3;
        int v7 = v6 + -3;
        int v8 = v7 + -2;
        int v9 = v8 + -1;
        int v10 = v9 + 0;
        int v11 = v10 + 1;
        int v12 = v11 + 2;
        int v13 = v12 + 3;
        int v14 = v13 + -3;
        int v15 = v14 + -2;
        int v16 = v15 + -1;
        int v17 = v16 + 0;
        int v18 = v17 + 1;
        int v19 = v18 + 2;
        int v20 = v19 + 3;
        int v21 = v20 + -3;
        int v22 = v21 + -2;
        int v23 = v22 + -1;
        int v24 = v23 + 0;
        int v25 = v24 + 1;
        int v26 = v25 + 2;
        int v27 = v26 + 3;
        int v28 = v27 + -3;
        int v29 = v28 + -2;
        int v30 = v29 + -1;
        int v31 = v30 + 0;
        int v32 = v31 + 1;
        int v33 = v32 + 2;
        int v34 = v33 + 3;
        int v35 = v34 + -3;
        int v36 = v35 + -2;
        int v37 = v36 + -1;
        int v38 = v37 + 0;
        int v39 = v38 + 1;
        int v40 = v39 + 2;
        int v41 = v40 + 3;
        int v42 = v41 + -3;
        int v43 = v42 + -2;
        int v44 = v43 + -1;
        int v45 = v44 + 0;
        int v46 = v45 + 1;
        int v47 = v46 + 2;
        int v48 = v47 + 3;
        int v49 = v48 + -3;
        int v50 = v49 + -2;
        int v51 = v50 + -1;
        int v52 = v51 + 0;
        int v53 = v52 + 1;
        int v54 = v53 + 2;
        int v55 = v54 + 3;
        int v56 = v55 + -3;
        int v57 = v56 + -2;
        int v58 = v57 + -1;
        int v59 = v58 + 0;
        int v60 = v59 + 1;
        int v61 = v60 + 2;
        int v62 = v61 + 3;
        int v63 = v62 + -3;
        int v64 = v63 + -2;
        int v65 = v64 + -1;
        int v66 = v65 + 0;
        int v67 = v66 + 1;
        int v68 = v67 + 2;
        int v69 = v68 + 3;
        int v70 = v69 + -3;
        int v71 = v70 + -2;
        int v72 = v71 + -1;
        int v73 = v72 + 0;
        int v74 = v73 + 1;
        int v75 = v74 + 2;
        int v76 = v75 + 3;
        int v77 = v76 + -3;
        int v78 = v77 + -2;
        int v79 = v78 + -1;
        int v80 = v79 + 0;
        int v81 = v80 + 1;
        int v82 = v81 + 2;
        int v83 = v82 + 3;
        int v84 = v83 + -3;
        int v85 = v84 + -2;
        int v86 = v85 + -1;
        int v87 = v86 + 0;
        int v88 = v87 + 1;
        int v89 = v88 + 2;
        int v90 = v89 + 3;
        int v91 = v90 + -3;
        int v92 = v91 + -2;
        int v93 = v92 + -1;
        int v94 = v93 + 0;
        int v95 = v94 + 1;
        int v96 = v95 + 2;
        int v97 = v96 + 3;
        int v98 = v97 + -3;
        int v99 = v98 + -2;
        int v100 = v99 + -1;
        int v101 = v100 + 0;
        int v102 = v101 + 1;
        int v103 = v102 + 2;
        int v104 = v103 + 3;
        int v105 = v104 + -3;
        int v106 = v105 + -2;
        int v107 = v106 + -1;
        int v108 = v107 + 0;
        int v109 = v108 + 1;
        int v110 = v109 + 2;
        int v111 = v110 + 3;
        int v112 = v111 + -3;
        int v113 = v112 + -2;
        int v114 = v113 + -1;
        int v115 = v114 + 0;
        int v116 = v115 + 1;
        int v117 = v116 + 2;
        int v118 = v117 + 3;
        int v119 = v118 + -3;
        int v120 = v119 + -2;
        int v121 = v120 + -1;
        int v122 = v121 + 0;
        int v123 = v122 + 1;
        int v124 = v123 + 2;
        int v125 = v124 + 3;
        int v126 = v125 + -3;
        int v127 = v126 + -2;
        int v128 = v127 + -1;
        int v129 = v128 + 0;
        int v130 = v129 + 1;
        int v131 = v130 + 2;
        int v132 = v131 + 3;
        int v133 = v132 + -3;
        int v134 = v133 + -2;
        int v135 = v134 + -1;
        int v136 = v135 + 0;
        int v137 = v136 + 1;
        int v138 = v137 + 2;
        int v139 = v138 + 3;
        int v140 = v139 + -3;
        int v141 = v140 + -2;
        int v142 = v141 + -1;
        int v143 = v142 + 0;
        int v144 = v143 + 1;
        int v145 = v144 + 2;
        int v146 = v145 + 3;
        int v147 = v146 + -3;
        int v148 = v147 + -2;
        int v149 = v148 + -1;
        int v150 = v149 + 0;
        int v151 = v150 + 1;
        int v152 = v151 + 2;
        int v153 = v152 + 3;
        int v154 = v153 + -3;
        int v155 = v154 + -2;
        int v156 = v155 + -1;
        int v157 = v156 + 0;
        int v158 = v157 + 1;
        int v159 = v158 + 2;
        int v160 = v159 + 3;
        int v161 = v160 + -3;
        int v162 = v161 + -2;
        int v163 = v162 + -1;
        int v164 = v163 + 0;
        int v165 = v164 + 1;
        int v166 = v165 + 2;
        int v167 = v166 + 3;
        int v168 = v167 + -3;
        int v169 = v168 + -2;
        int v170 = v169 + -1;
        int v171 = v170 + 0;
        int v172 = v171 + 1;
        int v173 = v172 + 2;
        int v174 = v173 + 3;
        int v175 = v174 + -3;
        int v176 = v175 + -2;
        int v177 = v176 + -1;
        int v178 = v177 + 0;
        int v179 = v178 + 1;
        int v180 = v179 + 2;
        int v181 = v180 + 3;
        int v182 = v181 + -3;
        int v183 = v182 + -2;
        int v184 = v183 + -1;
        int v185 = v184 + 0;
        int v186 = v185 + 1;
        int v187 = v186 + 2;
        int v188 = v187 + 3;
        int v189 = v188 + -3;
        int v190 = v189 + -2;
        int v191 = v190 + -1;
        int v192 = v191 + 0;
        int v193 = v192 + 1;
        int v194 = v193 + 2;
        int v195 = v194 + 3;
        int v196 = v195 + -3;
        int v197 = v196 + -2;
        int v198 = v197 + -1;
        int v199 = v198 + 0;
        int v200 = v199 + 1;
        int v201 = v200 + 2;
        int v202 = v201 + 3;
        int v203 = v202 + -3;
        int v204 = v203 + -2;
        int v205 = v204 + -1;
        int v206 = v205 + 0;
        int v207 = v206 + 1;
        int v208 = v207 + 2;
        int v209 = v208 + 3;
        int v210 = v209 + -3;
        int v211 = v210 + -2;
        int v212 = v211 + -1;
        int v213 = v212 + 0;
        int v214 = v213 + 1;
        int v215 = v214 + 2;
        int v216 = v215 + 3;
        int v217 = v216 + -3;
        int v218 = v217 + -2;
        int v219 = v218 + -1;
        int v220 = v219 + 0;
        int v221 = v220 + 1;
        int v222 = v221 + 2;
        int v223 = v222 + 3;
        int v224 = v223 + -3;
        int v225 = v224 + -2;
        int v226 = v225 + -1;
        int v227 = v226 + 0;
        int v228 = v227 + 1;
        int v229 = v228 + 2;
        int v230 = v229 + 3;
        int v231 = v230 + -3;
        int v232 = v231 + -2;
        int v233 = v232 + -1;
        int v234 = v233 + 0;
        int v235 = v234 + 1;
        int v236 = v235 + 2;
        int v237 = v236 + 3;
        int v238 = v237 + -3;
        int v239 = v238 + -2;
        int v240 = v239 + -1;
        int v241 = v240 + 0;
        int v242 = v241 + 1;
        int v243 = v242 + 2;
        int v244 = v243 + 3;
        int v245 = v244 + -3;
        int v246 = v245 + -2;
        int v247 = v246 + -1;
        int v248 = v247 + 0;
        int v249 = v248 + 1;
        int v250 = v249 + 2;
        int v251 = v250 + 3;
        int v252 = v251 + -3;
        int v253 = v252 + -2;
        int v254 = v253 + -1;
        int v255 = v254 + 0;
        int v256 = v255 + 1;
        int v257 = v256 + 2;
        int v258 = v257 + 3;
        int v259 = v258 + -3;
        int v260 = v259 + -2;
        int v261 = v260 + -1;
        // The loop lives entirely in the high slots: `wide iload`, `wide istore`,
        // `wide iinc`, and a `wide iinc` with a 16-bit constant.
        for (int i = 0; i < 20; i++) {
            v261 = v261 + v260;
            v261 += 300;
            v260 -= v259;
        }
        return v261 + v260 + v259 + v0;
    }

    public static int run() {
        int score = 0;
        for (int round = 0; round < 200; round++) {
            score += bump(round);
            score += deep(round);
        }
        return score;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
