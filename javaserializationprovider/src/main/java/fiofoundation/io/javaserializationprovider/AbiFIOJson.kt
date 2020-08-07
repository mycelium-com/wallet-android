package fiofoundation.io.javaserializationprovider



object AbiFIOJson {
    val abiFioJsonMap = initAbiFioJsonMap()

    private fun initAbiFioJsonMap(): Map<String, String> {
        val jsonMap = mutableMapOf<String, String>()

        jsonMap["abi.abi.json"] = "{\n" +
                "    \"version\": \"eosio::abi/1.1\",\n" +
                "    \"structs\": [\n" +
                "        {\n" +
                "            \"name\": \"extensions_entry\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"tag\",\n" +
                "                    \"type\": \"uint16\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"value\",\n" +
                "                    \"type\": \"bytes\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"type_def\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"new_type_name\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"type\",\n" +
                "                    \"type\": \"string\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"field_def\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"type\",\n" +
                "                    \"type\": \"string\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"struct_def\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"base\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"fields\",\n" +
                "                    \"type\": \"field_def[]\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"action_def\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"type\": \"name\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"type\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"ricardian_contract\",\n" +
                "                    \"type\": \"string\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"table_def\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"type\": \"name\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"index_type\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"key_names\",\n" +
                "                    \"type\": \"string[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"key_types\",\n" +
                "                    \"type\": \"string[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"type\",\n" +
                "                    \"type\": \"string\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"clause_pair\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"id\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"body\",\n" +
                "                    \"type\": \"string\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"error_message\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"error_code\",\n" +
                "                    \"type\": \"uint64\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"error_msg\",\n" +
                "                    \"type\": \"string\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"variant_def\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"types\",\n" +
                "                    \"type\": \"string[]\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"abi_def\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"version\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"types\",\n" +
                "                    \"type\": \"type_def[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"structs\",\n" +
                "                    \"type\": \"struct_def[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"actions\",\n" +
                "                    \"type\": \"action_def[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"tables\",\n" +
                "                    \"type\": \"table_def[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"ricardian_clauses\",\n" +
                "                    \"type\": \"clause_pair[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"error_messages\",\n" +
                "                    \"type\": \"error_message[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"abi_extensions\",\n" +
                "                    \"type\": \"extensions_entry[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"variants\",\n" +
                "                    \"type\": \"variant_def[]$\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}"

        jsonMap["eosio.assert.abi.json"] = "{\n" +
                "   \"version\": \"eosio::abi/1.0\",\n" +
                "   \"structs\": [\n" +
                "      {\n" +
                "         \"name\": \"chain_params\",\n" +
                "         \"base\": \"\",\n" +
                "         \"fields\": [\n" +
                "            {\n" +
                "               \"name\": \"chain_id\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"chain_name\",\n" +
                "               \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"icon\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            }\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"stored_chain_params\",\n" +
                "         \"base\": \"\",\n" +
                "         \"fields\": [\n" +
                "            {\n" +
                "               \"name\": \"chain_id\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"chain_name\",\n" +
                "               \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"icon\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"hash\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"next_unique_id\",\n" +
                "               \"type\": \"uint64\"\n" +
                "            }\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"contract_action\",\n" +
                "         \"base\": \"\",\n" +
                "         \"fields\": [\n" +
                "            {\n" +
                "               \"name\": \"contract\",\n" +
                "               \"type\": \"name\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"action\",\n" +
                "               \"type\": \"name\"\n" +
                "            }\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"manifest\",\n" +
                "         \"base\": \"\",\n" +
                "         \"fields\": [\n" +
                "            {\n" +
                "               \"name\": \"account\",\n" +
                "               \"type\": \"name\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"domain\",\n" +
                "               \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"appmeta\",\n" +
                "               \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"whitelist\",\n" +
                "               \"type\": \"contract_action[]\"\n" +
                "            }\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"stored_manifest\",\n" +
                "         \"base\": \"\",\n" +
                "         \"fields\": [\n" +
                "            {\n" +
                "               \"name\": \"unique_id\",\n" +
                "               \"type\": \"uint64\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"id\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"account\",\n" +
                "               \"type\": \"name\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"domain\",\n" +
                "               \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"appmeta\",\n" +
                "               \"type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"whitelist\",\n" +
                "               \"type\": \"contract_action[]\"\n" +
                "            }\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"del.manifest\",\n" +
                "         \"base\": \"\",\n" +
                "         \"fields\": [\n" +
                "            {\n" +
                "               \"name\": \"id\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            }\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"require\",\n" +
                "         \"base\": \"\",\n" +
                "         \"fields\": [\n" +
                "            {\n" +
                "               \"name\": \"chain_params_hash\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"manifest_id\",\n" +
                "               \"type\": \"checksum256\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"actions\",\n" +
                "               \"type\": \"contract_action[]\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"name\": \"abi_hashes\",\n" +
                "               \"type\": \"checksum256[]\"\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   ],\n" +
                "   \"actions\": [\n" +
                "      {\n" +
                "         \"name\": \"setchain\",\n" +
                "         \"type\": \"chain_params\",\n" +
                "         \"ricardian_contract\": \"\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"add.manifest\",\n" +
                "         \"type\": \"manifest\",\n" +
                "         \"ricardian_contract\": \"\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"del.manifest\",\n" +
                "         \"type\": \"del.manifest\",\n" +
                "         \"ricardian_contract\": \"\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"require\",\n" +
                "         \"type\": \"require\",\n" +
                "         \"ricardian_contract\": \"\"\n" +
                "      }\n" +
                "   ],\n" +
                "   \"tables\": [\n" +
                "      {\n" +
                "         \"name\": \"chain.params\",\n" +
                "         \"type\": \"stored_chain_params\",\n" +
                "         \"index_type\": \"i64\",\n" +
                "         \"key_names\": [\n" +
                "            \"key\"\n" +
                "         ],\n" +
                "         \"key_types\": [\n" +
                "            \"uint64\"\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"name\": \"manifests\",\n" +
                "         \"type\": \"stored_manifest\",\n" +
                "         \"index_type\": \"i64\",\n" +
                "         \"key_names\": [\n" +
                "            \"key\"\n" +
                "         ],\n" +
                "         \"key_types\": [\n" +
                "            \"uint64\"\n" +
                "         ]\n" +
                "      }\n" +
                "   ],\n" +
                "   \"ricardian_clauses\": [],\n" +
                "   \"abi_extensions\": []\n" +
                "}"

        jsonMap["transaction.abi.json"] = "{\n" +
                "    \"version\": \"eosio::abi/1.0\",\n" +
                "    \"types\": [\n" +
                "        {\n" +
                "            \"new_type_name\": \"account_name\",\n" +
                "            \"type\": \"name\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"new_type_name\": \"action_name\",\n" +
                "            \"type\": \"name\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"new_type_name\": \"permission_name\",\n" +
                "            \"type\": \"name\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"structs\": [\n" +
                "        {\n" +
                "            \"name\": \"permission_level\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"actor\",\n" +
                "                    \"type\": \"account_name\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"permission\",\n" +
                "                    \"type\": \"permission_name\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"action\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"account\",\n" +
                "                    \"type\": \"account_name\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"type\": \"action_name\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"authorization\",\n" +
                "                    \"type\": \"permission_level[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"data\",\n" +
                "                    \"type\": \"bytes\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"extension\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"type\",\n" +
                "                    \"type\": \"uint16\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"data\",\n" +
                "                    \"type\": \"bytes\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"transaction_header\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"expiration\",\n" +
                "                    \"type\": \"time_point_sec\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"ref_block_num\",\n" +
                "                    \"type\": \"uint16\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"ref_block_prefix\",\n" +
                "                    \"type\": \"uint32\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"max_net_usage_words\",\n" +
                "                    \"type\": \"varuint32\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"max_cpu_usage_ms\",\n" +
                "                    \"type\": \"uint8\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"delay_sec\",\n" +
                "                    \"type\": \"varuint32\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"transaction\",\n" +
                "            \"base\": \"transaction_header\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"context_free_actions\",\n" +
                "                    \"type\": \"action[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"actions\",\n" +
                "                    \"type\": \"action[]\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"transaction_extensions\",\n" +
                "                    \"type\": \"extension[]\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}"

        jsonMap["fio.abi.json"] = "{\n" +
                "    \"version\": \"eosio::abi/1.0\",\n" +
                "    \"structs\": [\n" +
                "        {\n" +
                "            \"name\": \"new_funds_content\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"payee_public_address\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"amount\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"chain_code\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"token_code\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"memo\",\n" +
                "                    \"type\": \"string?\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"hash\",\n" +
                "                    \"type\": \"string?\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"offline_url\",\n" +
                "                    \"type\": \"string?\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"record_obt_data_content\",\n" +
                "            \"base\": \"\",\n" +
                "            \"fields\": [\n" +
                "                {\n" +
                "                    \"name\": \"payer_public_address\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"payee_public_address\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"amount\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"chain_code\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"token_code\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"status\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"obt_id\",\n" +
                "                    \"type\": \"string\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"memo\",\n" +
                "                    \"type\": \"string?\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"hash\",\n" +
                "                    \"type\": \"string?\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"offline_url\",\n" +
                "                    \"type\": \"string?\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}"

        return jsonMap
    }
}